package com.example.githubstats.scheduler;

import com.example.githubstats.service.FetchOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final FetchOrchestrationService fetchOrchestrationService;

    public ScheduledTasks(FetchOrchestrationService fetchOrchestrationService) {
        this.fetchOrchestrationService = fetchOrchestrationService;
    }

    /**
     * Scheduled task to trigger processing for ALL configured sources (GitHub,
     * Bitbucket).
     * Runs every Saturday at 2:00 AM Central Time.
     */
    @Scheduled(cron = "0 0 2 * * SAT", zone = "America/Chicago") // Adjust time/zone as needed
    public void triggerWeeklyFetchAllSources() {
        log.info("Scheduled Task: Triggering weekly stats fetch for ALL sources.");
        try {
            // Call the main orchestrator method that handles both sources
            fetchOrchestrationService.triggerAllProcessing();
            log.info("Scheduled Task: Asynchronous fetch process triggered successfully for all sources.");
        } catch (Exception e) {
            log.error("Scheduled Task: Failed to trigger the asynchronous fetch process for all sources.", e);
            // Optional: Log this trigger failure to ErrorLog table via ErrorLoggingService
            // if injected here
        }
    }
}