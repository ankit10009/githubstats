package com.example.githubstats.scheduler;

import com.example.githubstats.service.FetchOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component // Marks this as a Spring-managed component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final FetchOrchestrationService fetchOrchestrationService;

    // Inject the service needed to trigger the process
    public ScheduledTasks(FetchOrchestrationService fetchOrchestrationService) {
        this.fetchOrchestrationService = fetchOrchestrationService;
    }

    /**
     * Scheduled task to automatically trigger the GitHub stats fetch process every Saturday.
     * Runs at 2:00 AM Central Time (adjust cron/zone as needed).
     * Cron format: second minute hour day-of-month month day-of-week
     * Zone: Specifies the time zone for the schedule. "America/Chicago" covers Irving, TX.
     */
    @Scheduled(cron = "0 0 2 * * SAT", zone = "America/Chicago") // Run at 2:00:00 AM on Saturday in Central Time
    public void triggerWeeklyGithubFetch() {
        log.info("Scheduled task running: Triggering weekly GitHub stats fetch for all filters.");
        try {
            // Call the same asynchronous method used by the controller/runner
            fetchOrchestrationService.triggerAllFilterProcessing();
            log.info("Scheduled task: Asynchronous fetch process triggered successfully.");
        } catch (Exception e) {
            // Log any error during the *triggering* of the task by the scheduler
            log.error("Scheduled task: Failed to trigger the asynchronous fetch process.", e);
            // Consider logging this failure to your ErrorLog table as well if critical
            // errorLoggingService.logError(null, "Scheduled Task Trigger", e); // Requires injecting ErrorLoggingService here too
        }
    }
}