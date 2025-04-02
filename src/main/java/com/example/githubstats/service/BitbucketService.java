package com.example.githubstats.service;

import com.example.githubstats.dto.bitbucket.BitbucketCommit;
import com.example.githubstats.dto.bitbucket.BitbucketPagedResponse;
import com.example.githubstats.dto.bitbucket.BitbucketRepository;
import com.example.githubstats.entity.CommitStats;
import com.example.githubstats.repository.CommitStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class BitbucketService {

    private static final Logger log = LoggerFactory.getLogger(BitbucketService.class);
    private static final String SOURCE_NAME = "BitbucketCloud"; // Source identifier

    private final RestTemplate restTemplate; // Can be null if disabled/config error
    private final CommitStatsRepository commitStatsRepository;
    private final ErrorLoggingService errorLoggingService;

    private final String workspaceSlug;

    // Use @Autowired on constructor for injection
    @Autowired
    public BitbucketService(@Qualifier("bitbucketRestTemplate") Optional<RestTemplate> restTemplateOptional,
            CommitStatsRepository commitStatsRepository,
            ErrorLoggingService errorLoggingService,
            @Value("${bitbucket.workspace:#{null}}") String workspaceSlug) {
        this.restTemplate = restTemplateOptional.orElse(null);
        this.commitStatsRepository = commitStatsRepository;
        this.errorLoggingService = errorLoggingService;
        this.workspaceSlug = workspaceSlug;
    }

    @Transactional // Use Spring's annotation
    public void fetchAndSaveStatsForFilter(String filterCriteria, LocalDateTime sinceDateTime) {
        if (this.restTemplate == null) {
            log.warn(
                    "Bitbucket RestTemplate is not available (disabled or configuration error). Skipping Bitbucket fetch for filter '{}'.",
                    filterCriteria);
            // errorLoggingService.logError(SOURCE_NAME, filterCriteria, "Bitbucket Client
            // Init", new IllegalStateException("Bitbucket client not
            // configured/enabled."));
            return;
        }
        if (workspaceSlug == null || workspaceSlug.isBlank()) {
            log.error(
                    "Bitbucket workspace (bitbucket.workspace) is not configured. Skipping Bitbucket fetch for filter '{}'.",
                    filterCriteria);
            errorLoggingService.logError(SOURCE_NAME, filterCriteria, "Bitbucket Workspace Config",
                    new IllegalStateException("Bitbucket workspace not configured."));
            return;
        }

        log.info("Starting Bitbucket stats fetch for workspace: {}, Filter: '{}', Since: {}",
                workspaceSlug, filterCriteria, sinceDateTime);

        String workspaceContext = SOURCE_NAME + " Workspace: " + workspaceSlug;
        String filterContext = workspaceContext + ", Filter: " + filterCriteria;

        // --- Fetch Repositories ---
        String repoSearchUrl = String.format("/repositories/%s", workspaceSlug);
        // Example filter: assumes filterCriteria is a name substring. Adjust 'q' as
        // needed.
        String initialQuery = String.format("name~\"%s\"", filterCriteria);
        UriComponentsBuilder repoUriBuilder = UriComponentsBuilder.fromPath(repoSearchUrl)
                .queryParam("q", initialQuery)
                .queryParam("pagelen", 50) // Adjust page size
                .queryParam("fields",
                        "values.slug,values.name,values.full_name,values.project.key,next,page,pagelen,size"); // Request
                                                                                                               // specific
                                                                                                               // fields
                                                                                                               // +
                                                                                                               // pagination
                                                                                                               // info

        boolean foundAnyRepos = false;
        String nextPageRepoUrl = repoUriBuilder.toUriString(); // Start with initial URL

        while (nextPageRepoUrl != null && !nextPageRepoUrl.isEmpty()) {
            log.debug("Fetching Bitbucket repositories page: {}", nextPageRepoUrl);
            try {
                // Use URI object for exchange to handle encoding properly
                URI currentUri = URI.create(nextPageRepoUrl); // URI needed for exchange method

                ResponseEntity<BitbucketPagedResponse<BitbucketRepository>> response = restTemplate.exchange(
                        currentUri, // Use URI object
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<BitbucketPagedResponse<BitbucketRepository>>() {
                        });

                BitbucketPagedResponse<BitbucketRepository> pagedRepoResponse = response.getBody();

                if (pagedRepoResponse != null && pagedRepoResponse.getValues() != null
                        && !pagedRepoResponse.getValues().isEmpty()) {
                    foundAnyRepos = true;
                    log.info("Processing {} Bitbucket repositories from page.", pagedRepoResponse.getValues().size());
                    for (BitbucketRepository repo : pagedRepoResponse.getValues()) {
                        String repoContext = filterContext + ", Repo: " + repo.getFullName();
                        try {
                            processRepositoryCommits(repo, sinceDateTime, filterCriteria, repoContext);
                        } catch (HttpClientErrorException | RestClientException e) {
                            log.error("Error processing commits for Bitbucket repo {}: {}", repo.getFullName(),
                                    e.getMessage());
                            // Log error and continue to next repo (or re-throw depending on severity)
                            errorLoggingService.logError(SOURCE_NAME, filterCriteria,
                                    repoContext + ", Action: Process Commits", e);
                        } catch (Exception e) {
                            log.error("Unexpected error processing commits for Bitbucket repo {}: {}",
                                    repo.getFullName(), e.getMessage(), e);
                            errorLoggingService.logError(SOURCE_NAME, filterCriteria,
                                    repoContext + ", Action: Process Commits", e);
                        }
                    }
                    // Get URL for the next page (relative path needs base added by RestTemplate, or
                    // use full URL)
                    nextPageRepoUrl = pagedRepoResponse.getNext();
                    if (nextPageRepoUrl != null)
                        log.debug("Next Bitbucket repository page URL: {}", nextPageRepoUrl);

                } else {
                    log.debug("No repository values found on this page or empty response body.");
                    nextPageRepoUrl = null; // Stop if no values or body
                }

            } catch (HttpClientErrorException e) {
                log.error("HTTP error fetching Bitbucket repositories for filter '{}': {} {}", filterCriteria,
                        e.getStatusCode(), e.getResponseBodyAsString());
                errorLoggingService.logError(SOURCE_NAME, filterCriteria, filterContext + ", Action: List Repos", e);
                nextPageRepoUrl = null; // Stop pagination on error
            } catch (RestClientException e) {
                log.error("Network/Client error fetching Bitbucket repositories for filter '{}': {}", filterCriteria,
                        e.getMessage());
                errorLoggingService.logError(SOURCE_NAME, filterCriteria, filterContext + ", Action: List Repos", e);
                nextPageRepoUrl = null; // Stop pagination on error
                // Consider re-throwing for fatal errors
            } catch (IllegalArgumentException e) {
                log.error("Error creating URI for repository pagination, potential issue with 'next' URL '{}': {}",
                        nextPageRepoUrl, e.getMessage());
                nextPageRepoUrl = null; // Stop if URL is invalid
            }
        } // End while loop for repo pages

        if (!foundAnyRepos)
            log.info("No Bitbucket repositories found for filter '{}'.", filterCriteria);
        log.info("Finished Bitbucket fetch for filter '{}'.", filterCriteria);
    }

    // Process commits for a single Bitbucket repository
    private void processRepositoryCommits(BitbucketRepository repo, LocalDateTime sinceDateTime, String filterCriteria,
            String repoContext) {
        log.debug("Listing Bitbucket commits for repository {} since {}", repo.getFullName(), sinceDateTime);

        String repoSlug = repo.getSlug();
        String commitListUrl = String.format("/repositories/%s/%s/commits", workspaceSlug, repoSlug);
        // Fetch specific fields to minimize payload
        UriComponentsBuilder commitUriBuilder = UriComponentsBuilder.fromPath(commitListUrl)
                .queryParam("pagelen", 50) // Adjust page size
                .queryParam("fields",
                        "values.hash,values.author,values.date,values.message,values.parents.size,next,page,pagelen,size"); // Select
                                                                                                                            // fields

        String nextPageCommitUrl = commitUriBuilder.toUriString();
        boolean continueFetching = true;
        OffsetDateTime sinceOffsetDateTime = sinceDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime(); // Convert
                                                                                                              // for
                                                                                                              // comparison

        while (continueFetching && nextPageCommitUrl != null && !nextPageCommitUrl.isEmpty()) {
            log.debug("Fetching Bitbucket commits page: {}", nextPageCommitUrl);
            try {
                URI currentUri = URI.create(nextPageCommitUrl); // Use URI object

                ResponseEntity<BitbucketPagedResponse<BitbucketCommit>> response = restTemplate.exchange(
                        currentUri,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<BitbucketPagedResponse<BitbucketCommit>>() {
                        });

                BitbucketPagedResponse<BitbucketCommit> pagedCommitResponse = response.getBody();

                if (pagedCommitResponse != null && pagedCommitResponse.getValues() != null
                        && !pagedCommitResponse.getValues().isEmpty()) {
                    log.debug("Processing {} Bitbucket commits from page.", pagedCommitResponse.getValues().size());
                    for (BitbucketCommit commit : pagedCommitResponse.getValues()) {
                        OffsetDateTime commitDate = commit.getDate();

                        // Stop fetching pages if we encounter a commit older than our target date
                        if (commitDate != null && commitDate.isBefore(sinceOffsetDateTime)) {
                            log.info("Commit {} ({}) is older than target date {}. Stopping pagination for repo {}.",
                                    commit.getHash().substring(0, 7), commitDate, sinceDateTime, repo.getFullName());
                            continueFetching = false;
                            break; // Stop processing this page
                        }

                        String sha = commit.getHash();
                        String repoFullName = repo.getFullName(); // Use workspace/slug
                        String commitContext = repoContext + ", Commit: " + sha.substring(0, 7);

                        // Check if commit already exists for this source
                        if (commitStatsRepository.findBySourceAndRepoNameAndSha(SOURCE_NAME, repoFullName, sha)
                                .isPresent()) {
                            log.trace("Bitbucket Commit {} in repo {} already exists. Skipping.", sha.substring(0, 7),
                                    repoFullName);
                            continue; // Skip this commit
                        }

                        // Extract data (handle potential nulls from DTO)
                        String authorName = commit.getAuthor() != null ? commit.getAuthor().getParsedName() : "N/A";
                        String authorEmail = commit.getAuthor() != null ? commit.getAuthor().getParsedEmail() : null; // Email
                                                                                                                      // often
                                                                                                                      // null
                        LocalDateTime localCommitDate = commitDate != null
                                ? commitDate.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
                                : null;

                        // TODO: Implement file count? Requires extra API call(s) for each commit's
                        // diffstat. Omitted for now.
                        Integer filesChanged = 0; // Placeholder
                        // TODO: Implement lines added/removed? Requires extra API call(s) for each
                        // commit's diffstat. Omitted for now.
                        Integer linesAdded = 0; // Placeholder
                        Integer linesRemoved = 0; // Placeholder

                        if (localCommitDate == null) {
                            log.warn("Commit date was null for Bitbucket commit {}. Skipping save.", sha);
                            continue;
                        }

                        CommitStats stats = new CommitStats(
                                SOURCE_NAME, repoFullName, sha,
                                authorName, authorEmail, localCommitDate,
                                linesAdded, linesRemoved, filesChanged // Using placeholders
                        );
                        commitStatsRepository.save(stats);
                        log.info("Saved Bitbucket stats for commit {} in repo {}", sha.substring(0, 7), repoFullName);
                    }
                    // Update URL for next page if we haven't stopped fetching
                    if (continueFetching) {
                        nextPageCommitUrl = pagedCommitResponse.getNext();
                        if (nextPageCommitUrl != null)
                            log.debug("Next Bitbucket commit page URL: {}", nextPageCommitUrl);
                    }

                } else {
                    log.debug("No commit values found on this page or empty response body for repo {}.",
                            repo.getFullName());
                    nextPageCommitUrl = null; // Stop if no values
                    continueFetching = false;
                }

            } catch (HttpClientErrorException e) {
                log.error("HTTP error fetching Bitbucket commits for repo '{}': {} {}", repo.getFullName(),
                        e.getStatusCode(), e.getResponseBodyAsString());
                errorLoggingService.logError(SOURCE_NAME, filterCriteria, repoContext + ", Action: List Commits", e);
                nextPageCommitUrl = null; // Stop pagination on error
                continueFetching = false;
            } catch (RestClientException e) {
                log.error("Network/Client error fetching Bitbucket commits for repo '{}': {}", repo.getFullName(),
                        e.getMessage());
                errorLoggingService.logError(SOURCE_NAME, filterCriteria, repoContext + ", Action: List Commits", e);
                nextPageCommitUrl = null; // Stop pagination on error
                continueFetching = false;
            } catch (IllegalArgumentException e) {
                log.error("Error creating URI for commit pagination, potential issue with 'next' URL '{}': {}",
                        nextPageCommitUrl, e.getMessage());
                nextPageCommitUrl = null; // Stop if URL is invalid
                continueFetching = false;
            }
        } // End while loop for commit pages
        log.debug("Finished processing Bitbucket commits for repository {}", repo.getFullName());
    }

} // End class BitbucketService