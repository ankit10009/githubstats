package com.example.githubstats.controller;

import com.example.githubstats.service.FetchOrchestrationService; // Import the new service
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class GitHubStatsController {

    private static final Logger log = LoggerFactory.getLogger(GitHubStatsController.class);

    private final FetchOrchestrationService fetchOrchestrationService; // Use final and constructor injection

    @Autowired // Optional with single constructor
    public GitHubStatsController(FetchOrchestrationService fetchOrchestrationService) {
        this.fetchOrchestrationService = fetchOrchestrationService;
    }

    /**
     * Endpoint to trigger the asynchronous processing of all configured repository filters.
     * Returns immediately after triggering the background task.
     *
     * @return ResponseEntity indicating the process has been accepted for execution.
     */
    @PostMapping("/trigger-fetch") // Changed endpoint path, no path variables needed
    public ResponseEntity<String> triggerFetchAllFilters() {
        log.info("Received request to trigger GitHub stats fetch for all filters.");
        try {
            // Call the asynchronous service method
            fetchOrchestrationService.triggerAllFilterProcessing();

            // Return immediately, indicating the process was started (Accepted status)
            return ResponseEntity.accepted().body("GitHub stats fetch process triggered asynchronously for all filters.");
        } catch (Exception e) {
            // Catch potential exceptions during the *triggering* itself (e.g., service unavailable)
            log.error("Failed to trigger the asynchronous fetch process: {}", e.getMessage(), e);
            // Log error using ErrorLoggingService if appropriate for trigger failures
            // errorLoggingService.logError(null, "Controller Trigger", e);
            return ResponseEntity.internalServerError().body("Failed to trigger fetch process: " + e.getMessage());
        }
    }
}