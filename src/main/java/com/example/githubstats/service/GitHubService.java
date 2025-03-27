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
import java.util.Optional;

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
            log.info("Found user {}",user.getLogin());

            // Get repositories using PagedIterable to handle potentially many repos
            PagedIterable<GHRepository> repositories = user.listRepositories().withPageSize(100);

            for(GHRepository repo : repositories){
                log.info("Processing repository: {}", repo.getFullName());
                processRepositoryCommits(repo);
            }
            log.info("Finished fetching stats for user {}", username);
        } catch (GHFileNotFoundException e) {
            log.error("GitHub user or resource not found: {}", username, e);
        }
//        catch (GHRateLimitExceededException e){
//            log.error("GitHub API Rate Limit Exceeded during commit fetch. Limit : {}. Remaining: {}, Resets at {}",
//                    e.getRateLimitLimit(), e.getRateLimitRemaining(), new Date(e.getRateLimitReset()*1000L), e);
//          // Implement backoff strategy here (e.g., wait until reset time)
//        }
        catch (IOException e) {
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
        } catch (IOException e) { // Catch the declared exception type
            log.warn("IOException encountered while trying to list commits or check iterator for repo {}: {}", repo.getFullName(), e.getMessage());

            // *** Check if the IOException is actually an HttpException ***
            if (e instanceof HttpException httpEx) { // Java 16+ pattern matching

                // It IS an HttpException, now check the status code
                if (httpEx.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) { // 409 status code
                    log.warn("Repository {} is confirmed empty via API response (HTTP 409). Skipping commit processing for this repo.", repo.getFullName());
                    return; // Exit this method gracefully, proceed to the next repository
                } else {
                    // It's a different *kind* of HttpException (e.g., 403 Forbidden, 404 Not Found, 500 Server Error)
                    log.error("Unexpected HttpException ({}) listing commits for repository {}: {}", httpEx.getResponseCode(), repo.getFullName(), httpEx.getMessage());
                    // Re-throw the original HttpException to be handled by the calling method's catch blocks
                    throw httpEx;
                }
            } else {
                // It's a different *kind* of IOException (e.g., network error, connection reset)
                log.error("Non-HTTP IOException trying to list commits for repository {}: {}", repo.getFullName(), e.getMessage());
                // Re-throw the original IOException
                throw e;
            }
        }

        log.debug("Fetching commit details for repository: {}", repo.getFullName());

        for(GHCommit listCommit : commits){
            String sha = listCommit.getSHA1();
            String repoName = repo.getFullName();

            if(commitStatsRepository.findByRepoNameAndSha(repoName,sha).isPresent()){
                log.debug("Commit {} in repo {} already exists  in DB. Skipping.",sha.substring(0,7),repo);
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

                // --- Rate Limit Consideration ---
                // Add a small delay to avoid hitting rate limits aggressively
                // Thread.sleep(50); // Sleep for 50ms (consider making this configurable)

            } catch (GHFileNotFoundException e) {
                log.warn("Commit {} in repo {} not found or repo structure changed. Skipping.", sha.substring(0, 7), repoName);
            }
//            catch (GHRateLimitExceededException e) {
//                log.error("GitHub API rate limit exceeded during commit fetch. Limit: {}, Remaining: {}, Resets at: {}",
//                        e.getRateLimitLimit(), e.getRateLimitRemaining(), new Date(e.getRateLimitReset()*1000L), e);
//                throw e; // Re-throw to stop processing in the outer loop or handle differently
//            }
            catch (IOException e) {
                log.error("IOException fetching commit details for {} in repo {}: {}", sha.substring(0, 7), repoName, e.getMessage());
                // Decide whether to continue with the next commit or stop
            }
//            catch (InterruptedException e) {
//                log.warn("Thread sleep interrupted.");
//                Thread.currentThread().interrupt(); // Restore interrupted status
//            }

        }
        log.debug("Finished processing commits for repository: {}", repo.getFullName());

    }
}
