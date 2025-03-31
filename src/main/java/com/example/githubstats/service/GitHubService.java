package com.example.githubstats.service;

import com.example.githubstats.config.GitHubConfig;
import com.example.githubstats.entity.CommitStats;
import com.example.githubstats.repository.CommitStatsRepository;
import jakarta.transaction.Transactional;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class GitHubService{
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    private final GitHub gitHub;
    private final CommitStatsRepository commitStatsRepository;
    private final ErrorLoggingService errorLoggingService; // Inject error logger

    @Value("${github.organization.name}")
    private String organizationName;

    public GitHubService(GitHubConfig gitHubConfig, GitHub gitHub, CommitStatsRepository commitStatsRepository, ErrorLoggingService errorLoggingService) {
        this.gitHub = gitHub;
        this.commitStatsRepository = commitStatsRepository;
        this.errorLoggingService = errorLoggingService;
    }

    @Transactional
    public void fetchAndSaveStatsForFilter(String filterCriteria, LocalDateTime sinceDateTime) throws IOException {
        log.info("Starting GitHub stats fetch for organization: {}, Filter: '{}', Since: {}",
                organizationName, filterCriteria, sinceDateTime);

        Instant instant = sinceDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Date sinceDate = Date.from(instant);
        String orgContext = "Organization: " + organizationName;

        try {
            GHOrganization organization = gitHub.getOrganization(organizationName);
            log.info("Found organization {}", organization.getLogin());

            // Get repositories using PagedIterable to handle potentially many repos
            PagedIterable<GHRepository> repositories = organization.listRepositories();
            repositories = repositories.withPageSize(100);

            for (GHRepository repo : repositories) {
                // Apply repository name filter based on the passed criteria
                if (!repo.getName().contains(filterCriteria)) {
                    log.trace("Skipping repository {} as it does not match CSI ID '{}'", repo.getFullName(), filterCriteria);
                    continue;
                }

                log.info("Processing repository: {} for CSI ID '{}'", repo.getFullName(), filterCriteria);

                String repoContext = orgContext + ", Repo: " + repo.getFullName();
                try {
                    processRepositoryCommits(repo, sinceDate, filterCriteria, repoContext);
                } catch (HttpException httpEx) {
                    String errorContext = repoContext + ", Action: List/Process Commits";
                    errorLoggingService.logError(filterCriteria, errorContext, httpEx); // Log the error
                    handleHttpException(httpEx, repo.getFullName()); // Log rate limit details etc.
                    if (httpEx.getResponseCode() == 429) {
                        log.warn("Rate limit hit processing repo {}. Stopping processing for filter '{}'.", repo.getFullName(), filterCriteria);
                        throw httpEx; // Re-throw to signal failure for this filter run
                    }
                    log.error("Skipping repository {} for filter '{}' due to HTTP error {}.", repo.getFullName(), filterCriteria, httpEx.getResponseCode());
                } catch (RuntimeException ghEx) {
                    String errorContext = repoContext + ", Action: List/Process Commits";
                    errorLoggingService.logError(filterCriteria, errorContext, ghEx); // Log the error
                    log.error("Error processing repository {} for filter '{}': {}. Skipping this repository.", repo.getFullName(), filterCriteria, ghEx.getMessage());
                } catch (IOException ioEx) {
                    String errorContext = repoContext + ", Action: List/Process Commits";
                    errorLoggingService.logError(filterCriteria, errorContext, ioEx); // Log the error
                    log.error("IOException processing repository {} for filter '{}': {}. Skipping this repository.", repo.getFullName(), filterCriteria, ioEx.getMessage());
                }
            }
            log.info("Finished fetching stats for organization {} with filter '{}'", organizationName, filterCriteria);
        } catch (GHFileNotFoundException e) {
            String errorContext = orgContext + ", Action: Get Organization";
            errorLoggingService.logError(filterCriteria, errorContext, e); // Log error
            log.error("GitHub organization not found: {}", organizationName);
            throw e; // Re-throw to indicate the run failed setup for this filter
        } catch (HttpException httpEx) {
            String errorContext = orgContext + ", Action: Get Organization/Initial Setup";
            errorLoggingService.logError(filterCriteria, errorContext, httpEx); // Log error
            handleHttpException(httpEx, "organization " + organizationName);
            throw httpEx; // Re-throw
        } catch (IOException e) {
            String errorContext = orgContext + ", Action: API Interaction";
            errorLoggingService.logError(filterCriteria, errorContext, e); // Log error
            log.error("IOException during GitHub API interaction for organization {}: {}", organizationName, e.getMessage());
            throw e;
        } catch (Exception e) {
            String errorContext = orgContext + ", Action: Unknown";
            errorLoggingService.logError(filterCriteria, errorContext, e); // Log error
            log.error("An unexpected error occurred while fetching stats for organization {} with filter '{}': {}", organizationName, filterCriteria, e.getMessage());
            throw new RuntimeException("Unexpected error during organization fetch for filter " + filterCriteria, e);
        }
    }

            // Helper method - Logs rate limit details (doesn't save to DB, ErrorLoggingService does that)
            private void handleHttpException(HttpException httpEx, String resourceIdentifier){
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
                    // Standard SLF4J logging; detailed logging is in ErrorLoggingService
                    log.warn("HTTP error ({}) processing {}: {}", httpEx.getResponseCode(), resourceIdentifier, httpEx.getMessage());
                }
            }

    private void processRepositoryCommits(GHRepository repo,Date sinceDate, String filterCriteria, String repoContext) throws IOException {

        log.debug("Listing commits for repository {} since {} for CSI ID '{}'", repo.getFullName(), sinceDate, filterCriteria);

        // 1. Initialize the query builder FIRST
        GHCommitQueryBuilder queryBuilder = repo.queryCommits();

        if (sinceDate != null) {
            queryBuilder.since(sinceDate);
        }

        PagedIterable<GHCommit> commits = queryBuilder.pageSize(100).list();

        try {
            if(!commits.iterator().hasNext()){
                log.info("Repository {} appears empty (iterator has no commits). Skipping.", repo.getFullName());
                return; // Nothing to process
            }
        }
        catch (RuntimeException re) {
            // Log error before potentially re-throwing
            errorLoggingService.logError(filterCriteria, repoContext + ", Action: List Commits (iterator check)", re);
            handleListCommitsException(re, repo.getFullName()); // This might re-throw specific exceptions
            return; // Return if handled gracefully (e.g., empty repo 409)
        }

        log.debug("Fetching commit details for repository: {}", repo.getFullName());

        for(GHCommit listCommit : commits){
            String sha = listCommit.getSHA1();
            String repoName = repo.getFullName();
            String commitContext = repoContext + ", Commit: " + sha.substring(0, 7);

            if(commitStatsRepository.findByRepoNameAndSha(repoName,sha).isPresent()){
                log.debug("Commit {} in repo {} already exists in DB. Skipping.",sha.substring(0,7),repo.getFullName());
                continue;
            }

            try {
                // --- Fetch Detailed Commit Info ---
                // Need to fetch the individual commit to get file stats reliably
                GHCommit detailedCommit = repo.getCommit(sha);
                GHCommit.ShortInfo commitInfo = detailedCommit.getCommitShortInfo();
                String authorName = "N/A";
                String authorEmail = "N/A";
                Date commitDateRaw = null;
                // Simplified author/date extraction (same as before)
                if(commitInfo != null && commitInfo.getAuthor() != null) {
                    authorName = commitInfo.getAuthor().getName();
                    authorEmail = commitInfo.getAuthor().getEmail();
                    commitDateRaw = commitInfo.getCommitDate();
                } else if (detailedCommit.getAuthor() != null) {
                    authorName = detailedCommit.getAuthor().getLogin();
                }
                // Use commit date first. If null, fallback to author date from the author object.
                if (commitDateRaw == null && commitInfo != null && commitInfo.getAuthor() != null) {
                    commitDateRaw = commitInfo.getAuthor().getDate();
                }
                // Final fallback if still null (should be rare)
                if (commitDateRaw == null) {
                    commitDateRaw = new Date();
                    log.warn("Commit date and author date were both null for commit {} in repo {}. Using current time as fallback.", sha.substring(0,7), repoName);
                }
                // Use commit date, not authoring date, if they differ. Convert Date to LocalDateTime.
                LocalDateTime commitDate = commitDateRaw.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                int filesChanged = detailedCommit.getFiles() != null ? detailedCommit.getFiles().size() : 0;
                int linesAdded = detailedCommit.getLinesAdded();
                int linesRemoved = detailedCommit.getLinesDeleted();

                CommitStats commitStatsEntity = new CommitStats(
                        repoName,
                        sha,
                        authorName,
                        authorEmail,
                        commitDate,
                        filesChanged,
                        linesAdded,
                        linesRemoved
                );

                commitStatsRepository.save(commitStatsEntity);
                log.info("Saved stats for commit {} in repo {}", sha.substring(0, 7), repoName);

            } catch (GHFileNotFoundException e) {
                errorLoggingService.logError(filterCriteria, commitContext + ", Action: Get Commit Detail", e);
                log.warn("Commit {} in repo {} not found. Skipping.", sha.substring(0, 7), repoName);
            } catch (IOException e) { // Includes HttpException
                errorLoggingService.logError(filterCriteria, commitContext + ", Action: Get Commit Detail", e);
                if (e instanceof HttpException httpEx) {
                    handleHttpException(httpEx, "commit " + sha.substring(0,7) + " in " + repoName);
                    if(httpEx.getResponseCode() == 429) throw e; // Propagate rate limit up
                    log.error("Skipping commit {} due to HTTP error.", sha.substring(0, 7));
                } else {
                    log.error("IOException fetching details for commit {} in repo {}. Skipping.", sha.substring(0, 7), repoName);
                }
            } catch (Exception e) {
                errorLoggingService.logError(filterCriteria, commitContext + ", Action: Process Commit Detail", e);
                log.error("Unexpected error processing commit {} in repo {}. Skipping.", sha.substring(0, 7), repoName, e);
            }
        }
        log.debug("Finished processing commits for repository: {}", repo.getFullName());
    }

    // Exception handler for listing commits (logs error via service and may re-throw)
    private void handleListCommitsException(RuntimeException re, String repoFullName) throws IOException {
        // No change needed here conceptually, but ensure errorLoggingService is called before re-throwing if needed
        log.warn("Exception encountered while listing commits or checking iterator for repo {}: {}", repoFullName, re.getMessage()); // Keep SLF4J log

        Throwable cause = re.getCause();
        // ... (rest of the logic is the same as before: check cause, handle 409, re-throw 429 or others) ...
        if (cause instanceof HttpException httpEx) {
            if (httpEx.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) { // 409
                log.warn("Repository {} might be empty (HTTP 409). Skipping commit processing.", repoFullName);
                return;
            } else if (httpEx.getResponseCode() == 429) { // 429 Rate Limit
                log.error("Rate limit exceeded (HTTP 429 wrapped) listing commits for repo {}.", repoFullName);
                if (re instanceof GHException) throw (GHException) re;
                else throw new IOException("Rate limit hit during commit listing", httpEx);
            } else {
                log.error("Unhandled HttpException ({}) wrapped during commit listing for repo {}.", httpEx.getResponseCode(), repoFullName, re);
                if (re instanceof GHException) throw (GHException) re;
                else throw new IOException("Unhandled HTTP error during commit listing", httpEx);
            }
        }
//        else if (re instanceof HttpException httpExDirect) { // Direct HttpException less likely here
//            handleHttpException(httpExDirect, "listing commits for " + repoFullName);
//            if (httpExDirect.getResponseCode() == 429 || httpExDirect.getResponseCode() == 409) { // Handle 409 too
//                if (httpExDirect.getResponseCode() == 409) {
//                    log.warn("Repository {} might be empty (Direct HTTP 409). Skipping commit processing.", repoFullName);
//                    return;
//                }
//                throw httpExDirect; // Re-throw 429
//            }
//            log.error("Unhandled direct HttpException ({}) listing commits. Re-throwing.", httpExDirect.getResponseCode());
//            throw httpExDirect;
//        }
        else {
            log.error("Other RuntimeException listing commits for repo {}. Re-throwing.", repoFullName, re);
            throw re;
        }
    }

}