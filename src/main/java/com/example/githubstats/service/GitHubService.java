package com.example.githubstats.service;

import com.example.githubstats.config.GitHubConfig;
import com.example.githubstats.entity.CommitStats;
import com.example.githubstats.repository.CommitStatsRepository;
import jakarta.transaction.Transactional;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class GitHubService{
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    private final GitHub gitHub;
    private final CommitStatsRepository commitStatsRepository;


    public GitHubService(GitHubConfig gitHubConfig, GitHub gitHub, CommitStatsRepository commitStatsRepository) {
        this.gitHub = gitHub;
        this.commitStatsRepository = commitStatsRepository;
    }

    @Transactional
    public void fetchAndSaveStatsForUser(String username) throws IOException {
        log.info("Starting GitHub stats fetch for user: {}", username);

        try {
            GHUser user = gitHub.getUser(username);
            if(user == null){
                log.error("User not found: {}", username);
                return;
            }
            log.info("Found user {}", user.getLogin());

            // Get repositories using PagedIterable to handle potentially many repos
            PagedIterable<GHRepository> repositories = user.listRepositories().withPageSize(100);

            for(GHRepository repo : repositories){
                log.info("Processing repository: {}", repo.getFullName());
                try {
                    processRepositoryCommits(repo);
                } catch (HttpException httpEx) {
                    if (httpEx.getResponseCode() == 429) {
                        // Handle rate limit exception
                        // Get rate limit info from GitHub if available
                        GHRateLimit rateLimit = gitHub.getRateLimit();
                        log.error("GitHub API Rate Limit Exceeded. Limit: {}, Remaining: {}, Resets at: {}",
                                rateLimit.getLimit(), rateLimit.getRemaining(),
                                new Date(rateLimit.getResetDate().getTime()));

                        // You could implement a backoff strategy here
                        // For example: wait until reset time or pause processing
                        log.info("Stopping processing due to rate limit. Consider implementing a waiting strategy.");
                        break; // Exit the repository loop
                    } else {
                        log.error("HTTP error ({}) processing repository {}: {}",
                                httpEx.getResponseCode(), repo.getFullName(), httpEx.getMessage());
                        // Decide whether to continue with next repo or not
                    }
                }
            }
            log.info("Finished fetching stats for user {}", username);
        } catch (GHFileNotFoundException e) {
            log.error("GitHub user or resource not found: {}", username, e);
        } catch (HttpException httpEx) {
            if (httpEx.getResponseCode() == 429) {
                // Handle rate limit exception at the user level
                GHRateLimit rateLimit = gitHub.getRateLimit();
                log.error("GitHub API Rate Limit Exceeded during user fetch. Limit: {}, Remaining: {}, Resets at: {}",
                        rateLimit.getLimit(), rateLimit.getRemaining(),
                        new Date(rateLimit.getResetDate().getTime()));
            } else {
                log.error("HTTP error ({}) during GitHub API interaction for user {}: {}",
                        httpEx.getResponseCode(), username, httpEx.getMessage());
            }
        } catch (GHIOException e) {
            log.error("IOException during GitHub API interaction for user {}: {}", username, e.getMessage(), e);
        } catch (Exception e) {
            log.error("An unexpected error occurred while fetching stats for user {}: {}", username, e.getMessage(), e);
        }
    }

    private void processRepositoryCommits(GHRepository repo) throws IOException {
        // Get commits using PagedIterable to handle potentially many commits
        PagedIterable<GHCommit> commits = null;
        try {
            commits = repo.listCommits().withPageSize(100);
            if(!commits.iterator().hasNext()){
                log.info("Repository {} appears empty (iterator has no commits). Skipping.", repo.getFullName());
                return; // Nothing to process
            }
        }
//        catch (IOException e) { // Catch direct IOExceptions first (like network errors, or direct HttpExceptions)
//            log.warn("IOException encountered while trying to list commits or check iterator for repo {}: {}", repo.getFullName(), e.getMessage());
//
//            if (e instanceof HttpException httpEx) {
//                // For empty repositories, GitHub API *sometimes* returns 409 directly as HttpException
//                if (httpEx.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) { // 409 status code
//                    log.warn("Repository {} is empty (HTTP 409 direct IOException). Skipping commit processing for this repo.", repo.getFullName());
//                    return; // Exit this method gracefully
//                }
//                // Check for rate limiting (HTTP 429)
//                else if (httpEx.getResponseCode() == 429) {
//                    log.error("GitHub API rate limit exceeded (HTTP 429 direct IOException) for repository {}. Re-throwing.", repo.getFullName());
//                    throw e; // Re-throw the original IOException
//                } else {
//                    // It's a different kind of direct HttpException
//                    log.error("Unexpected direct HttpException ({}) listing commits for repository {}: {}",
//                            httpEx.getResponseCode(), repo.getFullName(), httpEx.getMessage());
//                    throw e; // Re-throw the original IOException
//                }
//            } else {
//                // It's a different kind of direct IOException (network error, etc.)
//                log.error("Non-HTTP direct IOException trying to list commits for repository {}: {}", repo.getFullName(), e.getMessage());
//                throw e; // Re-throw
//            }
//        }
        catch (RuntimeException re) {
            log.warn("RuntimeException encountered while listing commits or checking iterator for repo {}: {}", repo.getFullName(), re.getMessage());

            // Specifically check if it's the GHException wrapping the 409 error for empty repos
            if (re instanceof GHException ghe && ghe.getCause() instanceof HttpException httpEx) {
                if (httpEx.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) {
                    log.warn("Repository {} is empty (HTTP 409 wrapped in GHException). Skipping commit processing.", repo.getFullName());
                    return; // Handled: Empty repo, exit method gracefully
                } else if (httpEx.getResponseCode() == 429) {
                    log.error("Rate limit exceeded (HTTP 429 wrapped in GHException) for repo {}. Re-throwing.", repo.getFullName());
                    // You might want specific rate limit handling here, but re-throwing is one option
                    throw re;
                }
                // Handle other wrapped HttpExceptions if necessary
                log.error("Unhandled HttpException ({}) wrapped in GHException for repo {}.", httpEx.getResponseCode(), repo.getFullName(), ghe);

            } else {
                // Log other runtime exceptions (could be other GHExceptions, NPEs, etc.)
                log.error("Other RuntimeException encountered for repo {}. See cause.", repo.getFullName(), re);
            }
            // Decide how to handle general/other runtime exceptions.
            // Re-throwing will stop processing for the user if not caught higher up.
            // Returning will skip the repo and continue with the next.
            // For safety, re-throwing might be better unless you know it's safe to continue.
            throw re; // Re-throw if not specifically handled (like the 409 case)
        }

        log.debug("Fetching commit details for repository: {}", repo.getFullName());

        for(GHCommit listCommit : commits){
            String sha = listCommit.getSHA1();
            String repoName = repo.getFullName();

            if(commitStatsRepository.findByRepoNameAndSha(repoName,sha).isPresent()){
                log.debug("Commit {} in repo {} already exists in DB. Skipping.",sha.substring(0,7),repo.getFullName());
                continue;
            }

            try {
                // --- Fetch Detailed Commit Info ---
                // Need to fetch the individual commit to get file stats reliably
                GHCommit detailedCommit = repo.getCommit(sha);
                GHCommit.ShortInfo commitInfo = detailedCommit.getCommitShortInfo();
                GHUser author = detailedCommit.getAuthor(); // May be null if author isn't a GH user
                String authorName = commitInfo.getAuthor().getName();
                String authorEmail = commitInfo.getAuthor().getEmail();
                // Use commit date, not authoring date, if they differ. Convert Date to LocalDateTime.
                LocalDateTime commitDate = commitInfo.getCommitDate().toInstant()
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
                log.warn("Commit {} in repo {} not found or repo structure changed. Skipping.", sha.substring(0, 7), repoName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        log.debug("Finished processing commits for repository: {}", repo.getFullName());
    }
}