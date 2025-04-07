package com.example.githubstats.service;

import com.example.githubstats.dto.bitbucketdc.*; // Import new DTOs
import com.example.githubstats.entity.CommitStats;
import com.example.githubstats.repository.CommitStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.time.Instant; // Use Instant for epoch millis
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class BitbucketService {

    private static final Logger log = LoggerFactory.getLogger(BitbucketService.class);
    private static final String SOURCE_NAME = "BitbucketDC"; // Source identifier

    private final RestTemplate restTemplate; // Can be null if disabled/config error
    private final CommitStatsRepository commitStatsRepository;
    private final ErrorLoggingService errorLoggingService;

    // No workspace needed for DC
    // private final String workspaceSlug;

    @Autowired
    public BitbucketService(@Qualifier("bitbucketRestTemplate") Optional<RestTemplate> restTemplateOptional,
                            CommitStatsRepository commitStatsRepository,
                            ErrorLoggingService errorLoggingService) {
        this.restTemplate = restTemplateOptional.orElse(null);
        this.commitStatsRepository = commitStatsRepository;
        this.errorLoggingService = errorLoggingService;
    }

    @Transactional
    public void fetchAndSaveStatsForFilter(String projectKey, LocalDateTime sinceDateTime) {
        if (this.restTemplate == null) {
            log.warn("Bitbucket DC RestTemplate is not available. Skipping fetch for project '{}'.", projectKey);
            return;
        }
        if (projectKey == null || projectKey.isBlank()) {
            log.error("Bitbucket DC Project Key (filter_criteria) is null or blank. Skipping fetch.");
            // Optionally log error via ErrorLoggingService
            return;
        }

        log.info("Starting Bitbucket DC stats fetch for Project Key: '{}', Since: {}", projectKey, sinceDateTime);
        String filterContext = SOURCE_NAME + " ProjectKey: " + projectKey;

        // --- Fetch Repositories by Project Key ---
        String repoListUrlTemplate = "/projects/{projectKey}/repos";
        int repoStart = 0;
        int repoLimit = 50; // Adjust as needed
        boolean repoLastPage = false;
        boolean foundAnyRepos = false;

        while (!repoLastPage) {
            UriComponentsBuilder repoUriBuilder = UriComponentsBuilder.fromPath(repoListUrlTemplate)
                    .queryParam("start", repoStart)
                    .queryParam("limit", repoLimit);

            String currentRepoUrl = repoUriBuilder.buildAndExpand(projectKey).toUriString();
            log.debug("Fetching Bitbucket DC repositories page: {}", currentRepoUrl);

            try {
                ResponseEntity<BitbucketDCPagedResponse<BitbucketDCRepository>> response = restTemplate.exchange(
                        currentRepoUrl, HttpMethod.GET, null,
                        new ParameterizedTypeReference<BitbucketDCPagedResponse<BitbucketDCRepository>>() {}
                );

                BitbucketDCPagedResponse<BitbucketDCRepository> pagedRepoResponse = response.getBody();

                if (pagedRepoResponse != null && pagedRepoResponse.getValues() != null && !pagedRepoResponse.getValues().isEmpty()) {
                    log.info("Processing {} Bitbucket DC repositories from page start={}", pagedRepoResponse.getValues().size(), repoStart);
                    for (BitbucketDCRepository repo : pagedRepoResponse.getValues()) {
                        foundAnyRepos = true;
                        String repoContext = filterContext + ", Repo: " + repo.getFullName();
                        try {
                            processRepositoryCommits(projectKey, repo, sinceDateTime, repoContext);
                        } catch (RestClientException e) {
                            log.error("API error processing commits for Bitbucket DC repo {}: {}", repo.getFullName(), e.getMessage());
                            errorLoggingService.logError(SOURCE_NAME, projectKey, repoContext + ", Action: Process Commits", e);
                        } catch (Exception e) {
                            log.error("Unexpected error processing commits for Bitbucket DC repo {}: {}", repo.getFullName(), e.getMessage(), e);
                            errorLoggingService.logError(SOURCE_NAME, projectKey, repoContext + ", Action: Process Commits", e);
                        }
                    }
                    // Update pagination vars for next loop
                    repoLastPage = pagedRepoResponse.isLastPage();
                    if (!repoLastPage && pagedRepoResponse.getNextPageStart() != null) {
                        repoStart = pagedRepoResponse.getNextPageStart();
                    } else {
                        repoLastPage = true; // Force stop if no next page info
                    }
                } else {
                    log.debug("No repositories found on this page or empty response.");
                    repoLastPage = true; // Stop if no values
                }
            } catch (HttpClientErrorException e) {
                log.error("HTTP error fetching Bitbucket DC repositories for project '{}': {} {}", projectKey, e.getStatusCode(), e.getResponseBodyAsString());
                errorLoggingService.logError(SOURCE_NAME, projectKey, filterContext + ", Action: List Repos", e);
                repoLastPage = true; // Stop pagination on error
            } catch (RestClientException e) {
                log.error("Network/Client error fetching Bitbucket DC repositories for project '{}': {}", projectKey, e.getMessage());
                errorLoggingService.logError(SOURCE_NAME, projectKey, filterContext + ", Action: List Repos", e);
                repoLastPage = true; // Stop pagination on error
            }
        } // End while repo pages

        if (!foundAnyRepos) log.info("No Bitbucket DC repositories found for project key '{}'.", projectKey);
        log.info("Finished Bitbucket DC fetch for project key '{}'.", projectKey);
    }


    private void processRepositoryCommits(String projectKey, BitbucketDCRepository repo, LocalDateTime sinceDateTime, String repoContext) {
        log.debug("Listing Bitbucket DC commits for repository {} since {}", repo.getFullName(), sinceDateTime);

        String repoSlug = repo.getSlug();
        String commitListUrlTemplate = "/projects/{projectKey}/repos/{repoSlug}/commits";
        int commitStart = 0;
        int commitLimit = 50; // Adjust page size
        boolean commitLastPage = false;
        long sinceEpochMillis = sinceDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        while (!commitLastPage) {
            UriComponentsBuilder commitUriBuilder = UriComponentsBuilder.fromPath(commitListUrlTemplate)
                    .queryParam("start", commitStart)
                    .queryParam("limit", commitLimit);
            // Note: Using 'sinceId' based on last stored SHA would be more robust here than date filtering
            // .queryParam("sinceId", lastProcessedShaForThisRepo);

            String currentCommitUrl = commitUriBuilder.buildAndExpand(projectKey, repoSlug).toUriString();
            log.debug("Fetching Bitbucket DC commits page: {}", currentCommitUrl);

            try {
                ResponseEntity<BitbucketDCPagedResponse<BitbucketDCCommit>> response = restTemplate.exchange(
                        currentCommitUrl, HttpMethod.GET, null,
                        new ParameterizedTypeReference<BitbucketDCPagedResponse<BitbucketDCCommit>>() {}
                );
                BitbucketDCPagedResponse<BitbucketDCCommit> pagedCommitResponse = response.getBody();

                if (pagedCommitResponse != null && pagedCommitResponse.getValues() != null && !pagedCommitResponse.getValues().isEmpty()) {
                    log.debug("Processing {} Bitbucket DC commits from page start={}", pagedCommitResponse.getValues().size(), commitStart);
                    for (BitbucketDCCommit commit : pagedCommitResponse.getValues()) {

                        // --- Filter by Date ---
                        long commitTimestamp = commit.getAuthorTimestamp(); // API uses authorTimestamp typically
                        if (commitTimestamp < sinceEpochMillis) {
                            log.info("Commit {} ({}) is older than target date {}. Stopping pagination for repo {}.",
                                    commit.getDisplayId(), Instant.ofEpochMilli(commitTimestamp), sinceDateTime, repo.getFullName());
                            commitLastPage = true; // Stop fetching pages
                            break; // Stop processing this page
                        }

                        // --- Skip Merge Commits ---
                        if (commit.isMergeCommit()) {
                            log.trace("Skipping Bitbucket DC merge commit {} in repo {}", commit.getDisplayId(), repo.getFullName());
                            continue;
                        }

                        String sha = commit.getId(); // Full SHA
                        String repoFullName = repo.getFullName();
                        String commitContext = repoContext + ", Commit: " + commit.getDisplayId();

                        // --- Check if exists in DB ---
                        if (commitStatsRepository.findBySourceAndRepoNameAndSha(SOURCE_NAME, repoFullName, sha).isPresent()) {
                            log.trace("Bitbucket DC Commit {} in repo {} already exists. Skipping.", commit.getDisplayId(), repoFullName);
                            continue;
                        }

                        // --- Fetch Change Stats ---
                        Integer totalLinesAdded = 0;
                        Integer totalLinesRemoved = 0;
                        Integer totalFilesChanged = 0;
                        try {
                            CommitDiffStats stats = fetchChangesStats(projectKey, repoSlug, sha, commitContext);
                            totalLinesAdded = stats.linesAdded();
                            totalLinesRemoved = stats.linesRemoved();
                            totalFilesChanged = stats.filesChanged();
                            log.debug("Change stats for commit {}: Added={}, Removed={}, Files={}", commit.getDisplayId(), totalLinesAdded, totalLinesRemoved, totalFilesChanged);
                        } catch (Exception e) {
                            log.error("Failed to fetch change stats for Bitbucket DC commit {}: {}. Storing 0/null for stats.", commit.getDisplayId(), e.getMessage());
                            // Error logged within fetchChangesStats
                            // Keep stats as 0/null
                        }

                        // --- Map and Save ---
                        String authorName = commit.getAuthor() != null ? commit.getAuthor().getName() : "N/A";
                        String authorEmail = commit.getAuthor() != null ? commit.getAuthor().getEmailAddress() : null;
                        LocalDateTime localCommitDate = Instant.ofEpochMilli(commitTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();

                        CommitStats commitStatsEntity = new CommitStats(
                                SOURCE_NAME, repoFullName, sha,
                                authorName, authorEmail, localCommitDate,
                                totalLinesAdded, totalLinesRemoved, totalFilesChanged
                        );
                        commitStatsRepository.save(commitStatsEntity);
                        log.info("Saved Bitbucket DC stats for commit {} in repo {}", commit.getDisplayId(), repoFullName);
                    }

                    // Update pagination vars for next loop IF we didn't break early due to date
                    if (!commitLastPage) {
                        commitLastPage = pagedCommitResponse.isLastPage();
                        if (!commitLastPage && pagedCommitResponse.getNextPageStart() != null) {
                            commitStart = pagedCommitResponse.getNextPageStart();
                        } else {
                            commitLastPage = true; // Force stop
                        }
                    }
                } else {
                    log.debug("No commit values found on this page or empty response body for repo {}.", repo.getFullName());
                    commitLastPage = true; // Stop if no values
                }

            } catch (HttpClientErrorException e) {
                log.error("HTTP error fetching Bitbucket DC commits for repo '{}': {} {}", repo.getFullName(), e.getStatusCode(), e.getResponseBodyAsString());
                errorLoggingService.logError(SOURCE_NAME, projectKey, repoContext + ", Action: List Commits", e);
                commitLastPage = true; // Stop pagination on error
            } catch (RestClientException e) {
                log.error("Network/Client error fetching Bitbucket DC commits for repo '{}': {}", repo.getFullName(), e.getMessage());
                errorLoggingService.logError(SOURCE_NAME, projectKey, repoContext + ", Action: List Commits", e);
                commitLastPage = true; // Stop pagination on error
            }
        } // End while commit pages
        log.debug("Finished processing Bitbucket DC commits for repository {}", repo.getFullName());
    }


    // Helper record for stats
    private record CommitDiffStats(int linesAdded, int linesRemoved, int filesChanged) {}

    // Fetch stats using the /changes endpoint
    private CommitDiffStats fetchChangesStats(String projectKey, String repoSlug, String commitSha, String commitContext) {
        log.debug("Fetching change stats for commit {}", commitSha.substring(0, 7));
        int totalLinesAdded = 0;
        int totalLinesRemoved = 0;
        int totalFilesChanged = 0;

        String changesUrlTemplate = "/projects/{projectKey}/repos/{repoSlug}/commits/{commitSha}/changes";
        int changesStart = 0;
        int changesLimit = 100; // Adjust as needed
        boolean changesLastPage = false;

        while (!changesLastPage) {
            UriComponentsBuilder changesUriBuilder = UriComponentsBuilder.fromPath(changesUrlTemplate)
                    .queryParam("start", changesStart)
                    .queryParam("limit", changesLimit);

            String currentChangesUrl = changesUriBuilder.buildAndExpand(projectKey, repoSlug, commitSha).toUriString();
            log.trace("Fetching change stats page: {}", currentChangesUrl);

            try {
                ResponseEntity<BitbucketDCChangesResponse> response = restTemplate.exchange(
                        currentChangesUrl, HttpMethod.GET, null,
                        new ParameterizedTypeReference<BitbucketDCChangesResponse>() {}
                );
                BitbucketDCChangesResponse pagedChangesResponse = response.getBody();

                if (pagedChangesResponse != null && pagedChangesResponse.getValues() != null) {
                    for (BitbucketDCChangeEntry entry : pagedChangesResponse.getValues()) {
                        totalLinesAdded += entry.getLinesAdded();
                        totalLinesRemoved += entry.getLinesRemoved();
                        totalFilesChanged++; // Count each entry
                    }
                    // Update pagination
                    changesLastPage = pagedChangesResponse.isLastPage();
                    if (!changesLastPage && pagedChangesResponse.getNextPageStart() != null) {
                        changesStart = pagedChangesResponse.getNextPageStart();
                    } else {
                        changesLastPage = true;
                    }
                } else {
                    log.debug("No change entries found on this page for commit {}.", commitSha.substring(0, 7));
                    changesLastPage = true;
                }

            } catch (HttpClientErrorException e) {
                log.error("HTTP error fetching change stats for commit {}: {} {}", commitSha.substring(0, 7), e.getStatusCode(), e.getResponseBodyAsString());
                errorLoggingService.logError(SOURCE_NAME, projectKey, commitContext + ", Action: Fetch Change Stats", e);
                changesLastPage = true; // Stop on error
                throw e; // Re-throw to indicate failure
            } catch (RestClientException e) {
                log.error("Network/Client error fetching change stats for commit {}: {}", commitSha.substring(0, 7), e.getMessage());
                errorLoggingService.logError(SOURCE_NAME, projectKey, commitContext + ", Action: Fetch Change Stats", e);
                changesLastPage = true; // Stop on error
                throw e; // Re-throw to indicate failure
            }
        } // End while changes pages

        return new CommitDiffStats(totalLinesAdded, totalLinesRemoved, totalFilesChanged);
    }

} // End class BitbucketService