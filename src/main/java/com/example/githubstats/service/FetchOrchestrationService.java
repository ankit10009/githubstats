package com.example.githubstats.service;

import com.example.githubstats.entity.RepositoryFilterControl;
import com.example.githubstats.repository.RepositoryFilterControlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Optional; // Import Optional

@Service
public class FetchOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(FetchOrchestrationService.class);
    private static final LocalDateTime DEFAULT_INITIAL_FETCH_DATE = LocalDateTime.of(2024, Month.JANUARY, 1, 0, 0, 0);
    private static final String GITHUB_SOURCE = "GitHub";
    private static final String BITBUCKET_SOURCE = "BitbucketCloud";

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

        for (RepositoryFilterControl filterControl : filtersToProcess) {
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
    }
}