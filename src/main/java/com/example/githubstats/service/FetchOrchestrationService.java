package com.example.githubstats.service;

import com.example.githubstats.entity.RepositoryFilterControl;
import com.example.githubstats.repository.RepositoryFilterControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional; // Import Optional

@Service
public class FetchOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(FetchOrchestrationService.class);
    private static final LocalDateTime DEFAULT_INITIAL_FETCH_DATE = LocalDateTime.of(2024, Month.JANUARY, 1, 0, 0, 0);
    private static final String GITHUB_SOURCE = "GitHub";
    private static final String BITBUCKET_SOURCE = "BitbucketDC";

    private final GitHubService gitHubService;
    private final BitbucketService bitbucketService; // Inject Bitbucket service
    private final RepositoryFilterControlRepository filterControlRepository;
    private final ErrorLoggingService errorLoggingService;

    @Value("${github.enabled:false}")
    private boolean githubEnabled;

    @Value("${bitbucket.enabled:false}")
    private boolean bitbucketEnabled;

    // Use constructor injection
    @Autowired
    public FetchOrchestrationService(Optional<GitHubService> gitHubServiceOpt, // Use Optional if beans might not exist
            Optional<BitbucketService> bitbucketServiceOpt,
            RepositoryFilterControlRepository filterControlRepository,
            ErrorLoggingService errorLoggingService) {
        this.gitHubService = gitHubServiceOpt.orElse(null); // Handle potential null beans
        this.bitbucketService = bitbucketServiceOpt.orElse(null);
        this.filterControlRepository = filterControlRepository;
        this.errorLoggingService = errorLoggingService;
    }

    @Async // Run the combined processing asynchronously
    public void triggerAllProcessing() {
        log.info("Processing trigger received. Checking enabled sources...");

        if (githubEnabled && gitHubService != null) {
            triggerAllFilterProcessing(GITHUB_SOURCE);
        } else {
            log.info("GitHub processing is disabled or service is unavailable.");
        }

        if (bitbucketEnabled && bitbucketService != null) {
            triggerAllFilterProcessing(BITBUCKET_SOURCE);
        } else {
            log.info("Bitbucket processing is disabled or service is unavailable.");
        }

        log.info("Finished processing trigger for all enabled sources.");
    }

    // Now processes filters for a specific source
    public void triggerAllFilterProcessing(String source) {
        log.info("Starting processing for source: {}", source);

        List<RepositoryFilterControl> filtersToProcess = filterControlRepository.findBySource(source);

        if (filtersToProcess.isEmpty()) {
            log.warn("No filters found in database for source '{}'.", source);
            // errorLoggingService.logError(source, null, "Orchestration Setup", new
            // IllegalStateException("No filters defined for source " + source));
            return;
        }

        log.info("Found {} filters to process for source '{}'.", filtersToProcess.size(), source);

        if (GITHUB_SOURCE.equals(source)) {
            // Process GitHub filters sequentially (or apply similar async pattern if needed)
            for (RepositoryFilterControl filterControl : filtersToProcess) {
                processSingleGitHubFilter(filterControl); // Assuming a sequential helper for now
            }
        } else if (BITBUCKET_SOURCE.equals(source)) {
            // Submit each Bitbucket project filter to the async executor
            log.info("Submitting {} Bitbucket project tasks for concurrent processing...", filtersToProcess.size());
            for (RepositoryFilterControl filterControl : filtersToProcess) {
                // Call the NEW async method for each Bitbucket project
                processSingleBitbucketProjectAsync(filterControl);
            }
            log.info("All Bitbucket project tasks submitted.");
        } else {
            log.warn("Unknown source '{}' encountered during dispatch.", source);
        }
        // Note: This method now returns quickly for Bitbucket after submitting tasks.
        log.info("Finished dispatching processing for source '{}'.", source);
    }

        /*for (RepositoryFilterControl filterControl : filtersToProcess) {
            String currentFilter = filterControl.getFilterCriteria();
            LocalDateTime currentFilterProcessingStartTime = LocalDateTime.now();
            LocalDateTime fetchSinceDateTime = filterControl.getLastFetchTimestamp() != null
                    ? filterControl.getLastFetchTimestamp()
                    : DEFAULT_INITIAL_FETCH_DATE;

            log.info("Processing filter: '{}' for source '{}'. Fetching commits since: {}", currentFilter, source,
                    fetchSinceDateTime);

            try {
                // Delegate to the appropriate service
                if (GITHUB_SOURCE.equals(source)) {
                    gitHubService.fetchAndSaveStatsForFilter(currentFilter, fetchSinceDateTime);
                } else if (BITBUCKET_SOURCE.equals(source)) {
                    bitbucketService.fetchAndSaveStatsForFilter(currentFilter, fetchSinceDateTime);
                } else {
                    log.warn("Unknown source '{}' encountered for filter '{}'. Skipping.", source, currentFilter);
                    continue; // Skip unknown sources
                }

                // If successful, update the timestamp for THIS filter
                filterControl.setLastFetchTimestamp(currentFilterProcessingStartTime);
                filterControlRepository.save(filterControl);
                log.info("Successfully processed filter '{}' for source '{}'. Updated timestamp to: {}", currentFilter,
                        source, currentFilterProcessingStartTime);

            } catch (Exception e) {
                String errorContext = String.format("Orchestration: Processing filter '%s' for source '%s'",
                        currentFilter, source);
                errorLoggingService.logError(source, currentFilter, errorContext, e);
                log.error("Failed to process filter '{}' for source '{}': {}. Timestamp remains unchanged ({}).",
                        currentFilter, source, e.getMessage(), fetchSinceDateTime, e); // Include exception in log
                // Continue to the next filter
            }
            log.info("-----------------------------------------------------");
        } // End loop

        log.info("Finished processing all filters for source '{}'.", source);

    }*/

    // --- Helper for sequential GitHub processing (or make async too if desired) ---
    private void processSingleGitHubFilter(RepositoryFilterControl filterControl) {
        String currentFilter = filterControl.getFilterCriteria();
        LocalDateTime fetchSinceDateTime = determineSinceDateTime(filterControl);
        LocalDateTime processingStartTime = LocalDateTime.now(); // Track start time
        log.info("(Sequential) Processing GitHub filter: '{}'. Fetching since: {}", currentFilter, fetchSinceDateTime);
        try {
            if (gitHubService != null) {
                gitHubService.fetchAndSaveStatsForFilter(currentFilter, fetchSinceDateTime);
                // Update timestamp on success
                filterControl.setLastFetchTimestamp(processingStartTime);
                filterControlRepository.save(filterControl);
                log.info("(Sequential) Successfully processed GitHub filter '{}'. Updated timestamp.", currentFilter);
            } else {
                log.warn("GitHub service is unavailable, skipping filter '{}'", currentFilter);
            }
        } catch (Exception e) {
            String errorContext = String.format("Orchestration: Processing GitHub filter '%s'", currentFilter);
            errorLoggingService.logError(GITHUB_SOURCE, currentFilter, errorContext, e);
            log.error("(Sequential) Failed to process GitHub filter '{}': {}. Timestamp NOT updated.", currentFilter, e.getMessage(), e);
        } finally {
            log.info("-----------------------------------------------------");
        }
    }

    // --- NEW Async method for processing a SINGLE Bitbucket project ---
    @Async("bitbucketTaskExecutor") // Specify the bean name of the dedicated executor
    @Transactional // Make processing for one project transactional
    public void processSingleBitbucketProjectAsync(RepositoryFilterControl filterControl) {
        String projectKey = filterControl.getFilterCriteria(); // filterCriteria is the projectKey
        LocalDateTime fetchSinceDateTime = determineSinceDateTime(filterControl);
        LocalDateTime processingStartTime = LocalDateTime.now(); // Track start time for this specific task

        // Log with thread name to see concurrency
        log.info("[{}] Starting async processing for Bitbucket project: '{}'. Fetching since: {}",
                Thread.currentThread().getName(), projectKey, fetchSinceDateTime);

        try {
            // Delegate the actual work for this project to the BitbucketService
            // The try-catch for service-level errors should be within BitbucketService ideally,
            // but we also catch here to ensure timestamp isn't updated on failure.
            if (bitbucketService != null) {
                bitbucketService.fetchAndSaveStatsForFilter(projectKey, fetchSinceDateTime);

                // If fetchAndSaveStatsForFilter completes without throwing an exception, update timestamp
                filterControl.setLastFetchTimestamp(processingStartTime);
                filterControlRepository.save(filterControl);
                log.info("[{}] Successfully processed Bitbucket project '{}'. Updated timestamp.", Thread.currentThread().getName(), projectKey);
            } else {
                log.warn("[{}] Bitbucket service is unavailable, skipping project '{}'", Thread.currentThread().getName(), projectKey);
                // Optionally log config error via ErrorLoggingService
            }

        } catch (Exception e) {
            // Log failure for this specific project. Error should also be logged deeper in BitbucketService.
            String errorContext = String.format("Async Orchestration: Processing Bitbucket project '%s'", projectKey);
            // Log again here if deeper logging might miss context or fail
            // errorLoggingService.logError(BITBUCKET_DC_SOURCE, projectKey, errorContext, e);
            log.error("[{}] Failed to process Bitbucket project '{}': {}. Timestamp NOT updated.",
                    Thread.currentThread().getName(), projectKey, e.getMessage(), e);
            // DO NOT update the timestamp here on failure
        } finally {
            log.info("[{}] Finished async task for Bitbucket project '{}'.", Thread.currentThread().getName(), projectKey);
            // Add separator if helps in logs, but might interleave with other threads
            // log.info("-----------------------------------------------------");
        }
    }

    // Helper to determine start date (used by both GitHub and Bitbucket processing)
    private LocalDateTime determineSinceDateTime(RepositoryFilterControl filterControl) {
        return filterControl.getLastFetchTimestamp() != null
                ? filterControl.getLastFetchTimestamp()
                : DEFAULT_INITIAL_FETCH_DATE;
    }
}