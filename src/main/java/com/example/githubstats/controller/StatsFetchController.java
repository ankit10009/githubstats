package com.example.githubstats.controller;

import com.example.githubstats.service.FetchOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // Import Value
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api") // Base path for API endpoints
public class StatsFetchController {

    private static final Logger log = LoggerFactory.getLogger(StatsFetchController.class);

    private final FetchOrchestrationService fetchOrchestrationService;

    // Inject config flags to check before triggering specific sources
    @Value("${github.enabled:false}")
    private boolean githubEnabled;
    @Value("${bitbucket.enabled:false}")
    private boolean bitbucketEnabled;

    public StatsFetchController(FetchOrchestrationService fetchOrchestrationService) {
        this.fetchOrchestrationService = fetchOrchestrationService;
    }

    /**
     * Triggers processing for ALL enabled sources (GitHub, Bitbucket).
     */
    @PostMapping("/trigger-all")
    public ResponseEntity<String> triggerFetchAll() {
        log.info("REST request to trigger fetch for ALL sources.");
        try {
            fetchOrchestrationService.triggerAllProcessing();
            return ResponseEntity.accepted()
                    .body("Stats fetch process triggered asynchronously for all enabled sources.");
        } catch (Exception e) {
            log.error("Failed to trigger 'triggerAllProcessing': {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to trigger fetch process: " + e.getMessage());
        }
    }

    /**
     * Triggers processing for GitHub sources only (if enabled).
     */
    @PostMapping("/github/trigger-fetch")
    public ResponseEntity<String> triggerFetchGitHub() {
        log.info("REST request to trigger fetch for GitHub sources.");
        if (!githubEnabled) {
            log.warn("GitHub processing is disabled. Trigger request ignored.");
            return ResponseEntity.badRequest().body("GitHub processing is disabled in configuration.");
        }
        try {
            // Call the orchestrator specifically for GitHub
            fetchOrchestrationService.triggerAllFilterProcessing("GitHub");
            return ResponseEntity.accepted().body("GitHub stats fetch process triggered asynchronously.");
        } catch (Exception e) {
            log.error("Failed to trigger GitHub processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to trigger GitHub fetch process: " + e.getMessage());
        }
    }

    /**
     * Triggers processing for Bitbucket sources only (if enabled).
     */
    @PostMapping("/bitbucket/trigger-fetch")
    public ResponseEntity<String> triggerFetchBitbucket() {
        log.info("REST request to trigger fetch for Bitbucket sources.");
        if (!bitbucketEnabled) {
            log.warn("Bitbucket processing is disabled. Trigger request ignored.");
            return ResponseEntity.badRequest().body("Bitbucket processing is disabled in configuration.");
        }
        try {
            // Call the orchestrator specifically for Bitbucket
            fetchOrchestrationService.triggerAllFilterProcessing("BitbucketCloud");
            return ResponseEntity.accepted().body("Bitbucket stats fetch process triggered asynchronously.");
        } catch (Exception e) {
            log.error("Failed to trigger Bitbucket processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to trigger Bitbucket fetch process: " + e.getMessage());
        }
    }
}