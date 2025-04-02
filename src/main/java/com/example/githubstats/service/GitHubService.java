package com.example.githubstats.service;

import com.example.githubstats.entity.CommitStats;
import com.example.githubstats.repository.CommitStatsRepository;
import org.kohsuke.github.*; // Keep GitHub imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Keep autowired or use constructor injection
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Use Spring's Transactional

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

@Service
public class GitHubService {
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private static final String SOURCE_NAME = "GitHub"; // Source identifier

    private final GitHub gitHub; // Can be null if disabled/config error
    private final CommitStatsRepository commitStatsRepository;
    private final ErrorLoggingService errorLoggingService;

    // Can be null if not configured
    private final String organizationName;

    // Constructor Injection Recommended
    @Autowired // Keep if needed, but constructor injection preferred
    public GitHubService(Optional<GitHub> gitHubOptional, // Use Optional for conditional bean
            CommitStatsRepository commitStatsRepository,
            ErrorLoggingService errorLoggingService,
            @Value("${github.organization.name:#{null}}") String organizationName) {
        this.gitHub = gitHubOptional.orElse(null); // Store null if GitHub client bean wasn't created
        this.commitStatsRepository = commitStatsRepository;
        this.errorLoggingService = errorLoggingService;
        this.organizationName = organizationName;
    }

    @Transactional // Use Spring's annotation
    public void fetchAndSaveStatsForFilter(String filterCriteria, LocalDateTime sinceDateTime) throws IOException {
        if (this.gitHub == null) {
            log.warn(
                    "GitHub client is not available (disabled or configuration error). Skipping GitHub fetch for filter '{}'.",
                    filterCriteria);
            // Optionally log to ErrorLog table
            // errorLoggingService.logError(SOURCE_NAME, filterCriteria, "GitHub Client
            // Init", new IllegalStateException("GitHub client not configured/enabled."));
            return; // Exit if client isn't configured
        }
        if (organizationName == null || organizationName.isBlank()) {
            log.error(
                    "GitHub organization name (github.organization.name) is not configured. Skipping GitHub fetch for filter '{}'.",
                    filterCriteria);
            errorLoggingService.logError(SOURCE_NAME, filterCriteria, "GitHub Org Config",
                    new IllegalStateException("GitHub organization not configured."));
            return;
        }

        log.info("Starting GitHub stats fetch for organization: {}, Filter: '{}', Since: {}",
                organizationName, filterCriteria, sinceDateTime);

        Instant instant = sinceDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date sinceDate = Date.from(instant);
        String orgContext = SOURCE_NAME + " Organization: " + organizationName;
        String filterContext = orgContext + ", Filter: " + filterCriteria;

        try {
            // Use Search API
            log.debug("Searching GitHub for repositories matching filter '{}' in organization '{}'", filterCriteria,
                    organizationName);
            String searchQuery = String.format("org:%s %s in:name", organizationName, filterCriteria);
            GHRepositorySearchBuilder searchBuilder = gitHub.searchRepositories().q(searchQuery);
            PagedSearchIterable<GHRepository> repositories = searchBuilder.list().pageSize(100); // Correct class

            log.info("Found GitHub repositories via search. Processing matches...");

            boolean foundAny = false;
            for (GHRepository repo : repositories) {
                foundAny = true;
                log.info("Processing GitHub repository: {} for filter '{}'", repo.getFullName(), filterCriteria);
                String repoContext = filterContext + ", Repo: " + repo.getFullName();
                try {
                    // Pass source name to commit processor
                    processRepositoryCommits(repo, sinceDate, filterCriteria, repoContext);
                } catch (HttpException httpEx) {
                    String errorContext = repoContext + ", Action: List/Process Commits";
                    errorLoggingService.logError(SOURCE_NAME, filterCriteria, errorContext, httpEx);
                    handleHttpException(httpEx, repo.getFullName());
                    if (httpEx.getResponseCode() == 429) {
                        log.warn("GitHub Rate limit hit processing repo {}. Stopping processing for filter '{}'.",
                                repo.getFullName(), filterCriteria);
                        throw httpEx;
                    }
                    log.error("Skipping GitHub repository {} for filter '{}' due to HTTP error {}.", repo.getFullName(),
                            filterCriteria, httpEx.getResponseCode());
                } catch (GHException | RuntimeException ghEx) {
                    String errorContext = repoContext + ", Action: List/Process Commits";
                    errorLoggingService.logError(SOURCE_NAME, filterCriteria, errorContext, ghEx);
                    log.error("Error processing GitHub repository {} for filter '{}': {}. Skipping.",
                            repo.getFullName(), filterCriteria, ghEx.getMessage());
                } catch (IOException ioEx) {
                    String errorContext = repoContext + ", Action: List/Process Commits";
                    errorLoggingService.logError(SOURCE_NAME, filterCriteria, errorContext, ioEx);
                    log.error("IOException processing GitHub repository {} for filter '{}': {}. Skipping.",
                            repo.getFullName(), filterCriteria, ioEx.getMessage());
                }
            }
            if (!foundAny)
                log.info("No GitHub repositories found via search for filter '{}'.", filterCriteria);
            log.info("Finished GitHub fetch for filter '{}'", filterCriteria);

        } catch (GHFileNotFoundException e) {
            String errorContext = filterContext + ", Action: Search Repos";
            errorLoggingService.logError(SOURCE_NAME, filterCriteria, errorContext, e);
            log.error("GitHub Search Error (Not Found) for filter '{}': {}", filterCriteria, e.getMessage(), e);
            throw e;
        } catch (HttpException httpEx) {
            String errorContext = filterContext + ", Action: Search Repos";
            errorLoggingService.logError(SOURCE_NAME, filterCriteria, errorContext, httpEx);
            handleHttpException(httpEx, "GitHub repository search for filter '" + filterCriteria + "'");
            throw httpEx;
        } catch (IOException e) {
            String errorContext = filterContext + ", Action: Search Repos";
            errorLoggingService.logError(SOURCE_NAME, filterCriteria, errorContext, e);
            log.error("IOException during GitHub repository search for filter '{}': {}", filterCriteria, e.getMessage(),
                    e);
            throw e;
        } catch (Exception e) {
            String errorContext = filterContext + ", Action: Unknown Setup/Search";
            errorLoggingService.logError(SOURCE_NAME, filterCriteria, errorContext, e);
            log.error("Unexpected error during GitHub search setup for filter '{}': {}", filterCriteria, e.getMessage(),
                    e);
            throw new RuntimeException("Unexpected error during GitHub setup for filter " + filterCriteria, e);
        }
    }

    // Modified to check existing commit using source
    private void processRepositoryCommits(GHRepository repo, Date sinceDate, String filterCriteria, String repoContext)
            throws IOException {
        log.debug("Listing GitHub commits for repository {} since {} for filter '{}'", repo.getFullName(), sinceDate,
                filterCriteria);

        GHCommitQueryBuilder queryBuilder = repo.listCommits();
        if (sinceDate != null)
            queryBuilder.since(sinceDate);
        PagedIterable<GHCommit> commits = queryBuilder.pageSize(100);

        try {
            if (!commits.iterator().hasNext()) {
                log.info("No new GitHub commits found in repository {} since {} for filter '{}'. Skipping.",
                        repo.getFullName(), sinceDate, filterCriteria);
                return;
            }
        } catch (RuntimeException re) {
            errorLoggingService.logError(SOURCE_NAME, filterCriteria,
                    repoContext + ", Action: List Commits (iterator check)", re);
            handleListCommitsException(re, repo.getFullName());
            return;
        }

        log.debug("Fetching GitHub commit details for repository: {}", repo.getFullName());
        for (GHCommit listCommit : commits) {
            String sha = listCommit.getSHA1();
            String repoFullName = repo.getFullName(); // Use full name consistently
            String commitContext = repoContext + ", Commit: " + sha.substring(0, 7);

            // Check if commit already exists FOR THIS SOURCE
            if (commitStatsRepository.findBySourceAndRepoNameAndSha(SOURCE_NAME, repoFullName, sha).isPresent()) {
                log.trace("GitHub Commit {} in repo {} already exists. Skipping.", sha.substring(0, 7), repoFullName);
                continue;
            }

            try {
                GHCommit detailedCommit = repo.getCommit(sha);
                GHCommit.ShortInfo commitInfo = detailedCommit.getCommitShortInfo();
                String authorName = "N/A";
                String authorEmail = "N/A";
                Date commitDateRaw = null;

                if (commitInfo != null && commitInfo.getAuthor() != null) {
                    authorName = commitInfo.getAuthor().getName();
                    authorEmail = commitInfo.getAuthor().getEmail();
                    commitDateRaw = commitInfo.getCommitDate(); // Use commit date
                    if (commitDateRaw == null && commitInfo.getAuthor() != null) {
                        commitDateRaw = commitInfo.getAuthor().getDate(); // Fallback to author date
                    }
                } else if (detailedCommit.getAuthor() != null) { // Fallback if ShortInfo is odd
                    authorName = detailedCommit.getAuthor().getLogin(); // Might only have login
                    // Try getting date from direct author/committer if needed - more complex
                }
                if (commitDateRaw == null)
                    commitDateRaw = new Date(); // Final fallback

                LocalDateTime commitDate = commitDateRaw.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                int linesAdded = detailedCommit.getLinesAdded();
                int linesRemoved = detailedCommit.getLinesDeleted();
                int filesChanged = detailedCommit.getFiles() != null ? detailedCommit.getFiles().size() : 0;

                // Create entity with source
                CommitStats stats = new CommitStats(SOURCE_NAME, repoFullName, sha, authorName, authorEmail, commitDate,
                        linesAdded, linesRemoved, filesChanged);
                commitStatsRepository.save(stats);
                log.info("Saved GitHub stats for commit {} in repo {}", sha.substring(0, 7), repoFullName);

            } catch (GHFileNotFoundException e) {
                errorLoggingService.logError(SOURCE_NAME, filterCriteria, commitContext + ", Action: Get Commit Detail",
                        e);
                log.warn("GitHub Commit {} in repo {} not found. Skipping.", sha.substring(0, 7), repoFullName);
            } catch (IOException e) {
                errorLoggingService.logError(SOURCE_NAME, filterCriteria, commitContext + ", Action: Get Commit Detail",
                        e);
                if (e instanceof HttpException httpEx) {
                    handleHttpException(httpEx, "commit " + sha.substring(0, 7) + " in " + repoFullName);
                    if (httpEx.getResponseCode() == 429)
                        throw e; // Propagate rate limit up
                    log.error("Skipping GitHub commit {} due to HTTP error.", sha.substring(0, 7));
                } else {
                    log.error("IOException fetching details for GitHub commit {} in repo {}. Skipping.",
                            sha.substring(0, 7), repoFullName);
                }
            } catch (Exception e) {
                errorLoggingService.logError(SOURCE_NAME, filterCriteria,
                        commitContext + ", Action: Process Commit Detail", e);
                log.error("Unexpected error processing GitHub commit {} in repo {}. Skipping.", sha.substring(0, 7),
                        repoFullName, e);
            }
        }
        log.debug("Finished processing GitHub commits for repository: {}", repo.getFullName());
    }

    // --- handleHttpException and handleListCommitsException remain the same ---
    private void handleHttpException(HttpException httpEx, String resourceIdentifier) {
        // ... (implementation from previous steps) ...
        if (httpEx.getResponseCode() == 429) {
            try {
                GHRateLimit rateLimit = gitHub.getRateLimit();
                log.warn("GitHub API Rate Limit Exceeded for {}. Limit: {}, Remaining: {}, Resets at: {}",
                        resourceIdentifier, rateLimit.getLimit(), rateLimit.getRemaining(),
                        new Date(rateLimit.getResetDate().getTime()));
            } catch (IOException ioException) {
                log.warn("Could not retrieve rate limit information after hitting limit for {}.", resourceIdentifier);
            }
        } else {
            log.warn("GitHub HTTP error ({}) processing {}: {}", httpEx.getResponseCode(), resourceIdentifier,
                    httpEx.getMessage());
        }
    }

    private void handleListCommitsException(RuntimeException re, String repoFullName) throws IOException {
        // ... (implementation from previous steps, ensure errors logged via service if
        // needed) ...
        log.warn("Exception listing GitHub commits for repo {}: {}", repoFullName, re.getMessage());
        Throwable cause = re.getCause();
        if (cause instanceof HttpException httpEx) {
            if (httpEx.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) { // 409
                log.warn("GitHub Repository {} might be empty (HTTP 409). Skipping commit processing.", repoFullName);
                return;
            } else if (httpEx.getResponseCode() == 429) { // 429 Rate Limit
                log.error("GitHub Rate limit exceeded (HTTP 429 wrapped) listing commits for repo {}.", repoFullName);
                if (re instanceof GHException)
                    throw (GHException) re;
                else
                    throw new IOException("GitHub Rate limit hit during commit listing", httpEx);
            } else {
                log.error("Unhandled GitHub HttpException ({}) wrapped during commit listing for repo {}.",
                        httpEx.getResponseCode(), repoFullName, re);
                if (re instanceof GHException)
                    throw (GHException) re;
                else
                    throw new IOException("Unhandled GitHub HTTP error during commit listing", httpEx);
            }
        } else if (re instanceof HttpException httpExDirect) {
            handleHttpException(httpExDirect, "listing GitHub commits for " + repoFullName);
            if (httpExDirect.getResponseCode() == 429 || httpExDirect.getResponseCode() == 409) {
                if (httpExDirect.getResponseCode() == 409) {
                    log.warn("GitHub Repository {} might be empty (Direct HTTP 409). Skipping commit processing.",
                            repoFullName);
                    return;
                }
                throw httpExDirect;
            }
            log.error("Unhandled direct GitHub HttpException ({}) listing commits. Re-throwing.",
                    httpExDirect.getResponseCode());
            throw httpExDirect;
        } else {
            log.error("Other RuntimeException listing GitHub commits for repo {}. Re-throwing.", repoFullName, re);
            throw re;
        }
    }

} // End class GitHubService