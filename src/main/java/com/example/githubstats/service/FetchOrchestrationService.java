package com.example.githubstats.service;

import com.example.githubstats.entity.RepositoryFilterControl;
import com.example.githubstats.repository.RepositoryFilterControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

@Service
public class FetchOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(FetchOrchestrationService.class);
    private static final LocalDateTime DEFAULT_INITIAL_FETCH_DATE = LocalDateTime.of(2024, Month.JANUARY, 1, 0, 0, 0);

    private final GitHubService gitHubService;
    private final RepositoryFilterControlRepository filterControlRepository;
    private final ErrorLoggingService errorLoggingService;

    // Constructor Injection
    public FetchOrchestrationService(GitHubService gitHubService,
                                     RepositoryFilterControlRepository filterControlRepository,
                                     ErrorLoggingService errorLoggingService) {
        this.gitHubService = gitHubService;
        this.filterControlRepository = filterControlRepository;
        this.errorLoggingService = errorLoggingService;
    }

    /**
     * Triggers the fetch and save process for all configured filters asynchronously.
     * This method contains the logic previously in the CommandLineRunner.
     */
    @Async // Marks this method for asynchronous execution
    public void triggerAllFilterProcessing() {
        log.info("Asynchronous processing triggered for all repository filters...");

        List<RepositoryFilterControl> filtersToProcess = filterControlRepository.findAll();

        if (filtersToProcess.isEmpty()) {
            log.warn("No repository filter criteria found in 'repository_filter_control' table. Aborting trigger.");
            errorLoggingService.logError(null, "FetchOrchestrationService", new IllegalStateException("No filters defined in repository_filter_control table."));
            return;
        }

        log.info("Found {} filters to process.", filtersToProcess.size());

        for (RepositoryFilterControl filterControl : filtersToProcess) {
            String currentFilter = filterControl.getFilterCriteria();
            // Record start time for *this filter's* processing attempt
            LocalDateTime currentFilterProcessingStartTime = LocalDateTime.now();

            LocalDateTime fetchSinceDateTime = filterControl.getLastFetchTimestamp() != null
                    ? filterControl.getLastFetchTimestamp()
                    : DEFAULT_INITIAL_FETCH_DATE;

            log.info("Processing filter: '{}'. Fetching commits since: {}", currentFilter, fetchSinceDateTime);

            try {
                // Call the core service method for the specific filter
                gitHubService.fetchAndSaveStatsForFilter(currentFilter, fetchSinceDateTime);

                // If successful, update the timestamp for THIS filter
                filterControl.setLastFetchTimestamp(currentFilterProcessingStartTime);
                filterControlRepository.save(filterControl);
                log.info("Successfully processed filter '{}'. Updated last fetch timestamp to: {}", currentFilter, currentFilterProcessingStartTime);

            } catch (Exception e) {
                // Log failure for this filter; ErrorLoggingService likely has specifics
                String errorContext = "FetchOrchestrationService: Processing filter " + currentFilter;
                errorLoggingService.logError(currentFilter, errorContext, e);
                log.error("Failed to process filter '{}': {}. Timestamp remains unchanged ({})",
                        currentFilter, e.getMessage(), fetchSinceDateTime);
                // Continue to the next filter
            }
            log.info("-----------------------------------------------------");
        } // End loop

        log.info("Finished asynchronous processing trigger for all filters.");
    }
}