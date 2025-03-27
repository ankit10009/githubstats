package com.example.githubstats.service;

import com.example.githubstats.repository.RepositoryStatsRepository;
import org.kohsuke.github.*; // Import necessary GitHub classes
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GitHubStatsService {

    private static final Logger log = LoggerFactory.getLogger(GitHubStatsService.class);
    private static final String ORG_NAME = "YOUR_ORG_NAME"; // Replace with your actual Org Name

    private final GitHub gitHub;
    // Add to GheStatsService class members:
    private final RepositoryStatsRepository repoStatsRepository;

    // Modify constructor:
    @Autowired
    public GitHubStatsService(GitHub gitHub, RepositoryStatsRepository repoStatsRepository) {
        this.gitHub = gitHub;
        this.repoStatsRepository = repoStatsRepository;
    }

    public void findAndProcessRepos(List<String> keywords) {
        log.info("Searching for repositories in org '{}' matching keywords: {}", ORG_NAME, keywords);
        Set<GHRepository> matchingRepos = findMatchingRepositoriesUsingSearch(keywords);
        // OR: Set<GHRepository> matchingRepos = findMatchingRepositoriesByListingAll(keywords);

        log.info("Found {} matching repositories. Processing stats...", matchingRepos.size());
        for (GHRepository repo : matchingRepos) {
            processRepositoryStats(repo);
        }
        log.info("Finished processing repositories.");
    }

    // --- Option A: Using Search API ---
    private Set<GHRepository> findMatchingRepositoriesUsingSearch(List<String> keywords) {
        Set<GHRepository> uniqueRepos = new HashSet<>();
        for (String keyword : keywords) {
            String query = String.format("org:%s %s in:name", ORG_NAME, keyword);
            log.debug("Executing search query: {}", query);
            try {
                PagedSearchIterable<GHRepository> results = gitHub.searchRepositories().q(query).list();
                results.withPageSize(100).forEach(repo -> {
                    log.trace("Search found: {}", repo.getFullName());
                    uniqueRepos.add(repo);
                });
            } catch (IOException e) {
                log.error("Error searching repositories with keyword '{}': {}", keyword, e.getMessage(), e);
                // Decide: continue with next keyword or stop?
            }
        }
        return uniqueRepos;
    }

    // --- Option B: Fallback - List All & Filter ---
    private Set<GHRepository> findMatchingRepositoriesByListingAll(List<String> keywords) {
        log.warn("Using inefficient method: Listing all repositories for org '{}' and filtering.", ORG_NAME);
        Set<GHRepository> uniqueRepos = new HashSet<>();
        try {
            GHOrganization org = gitHub.getOrganization(ORG_NAME);
            PagedIterable<GHRepository> allRepos = org.listRepositories().withPageSize(100);
            for (GHRepository repo : allRepos) {
                String repoNameLower = repo.getName().toLowerCase();
                for (String keyword : keywords) {
                    if (repoNameLower.contains(keyword.toLowerCase())) {
                        log.trace("Filter matched: {}", repo.getFullName());
                        uniqueRepos.add(repo);
                        break; // Match found for this repo, no need to check other keywords
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error listing repositories for org '{}': {}", ORG_NAME, e.getMessage(), e);
        }
        return uniqueRepos;
    }

    private void processRepositoryStats(GHRepository repo) {
        log.info("Processing stats for repository: {}", repo.getFullName());
        try {
            // --- Step 2: Data Extraction (To be implemented below) ---
            GHCommitActivityStats commitActivity = fetchCommitActivityWithRetry(repo);
            List<GHContributorStats> contributorStats = fetchContributorStatsWithRetry(repo);

            if (commitActivity == null || contributorStats == null) {
                log.warn("Could not retrieve complete stats for repo: {}", repo.getFullName());
                // Decide how to handle incomplete data (skip repo, save partial, etc.)
                return;
            }

            // Extract meaningful data
            int totalCommits = contributorStats.stream().mapToInt(GHContributorStats::getTotal).sum();
            int contributorCount = contributorStats.size();
            // Weekly activity is available in commitActivity.getWeeks()

            log.debug("Repo: {}, Total Commits: {}, Contributors: {}", repo.getFullName(), totalCommits, contributorCount);

            // --- Step 4: Database Output (To be implemented below) ---
            saveStatsToDatabase(repo, totalCommits, contributorCount, commitActivity, contributorStats);

        } catch (IOException e) {
            log.error("Failed to process stats for repo {}: {}", repo.getFullName(), e.getMessage(), e);
            // Handle specific exceptions like RateLimit, FileNotFound (repo deleted?)
            if (e instanceof GHRateLimitExceededException rateLimitEx) {
                long resetInSeconds = (rateLimitEx.getRateLimitReset()*1000L - System.currentTimeMillis()) / 1000;
                log.error("RATE LIMIT EXCEEDED processing {}. Resets in approx {} seconds. Consider adding delays.", repo.getFullName(), resetInSeconds);
                // You might want to pause execution here or stop processing.
            } else if (e instanceof GHFileNotFoundException) {
                log.warn("Repository {} seems to have been deleted or is inaccessible.", repo.getFullName());
            }
        } catch (InterruptedException e) {
            log.warn("Thread interrupted during stats processing for {}", repo.getFullName());
            Thread.currentThread().interrupt();
        }
    }
    // Add this method:
    private void saveStatsToDatabase(GHRepository repo, int totalCommits, int contributorCount,
                                     GHCommitActivityStats commitActivity, List<GHContributorStats> contributorStats) {
        String repoFullName = repo.getFullName();
        try {
            // Find existing or create new
            RepositoryStats stats = repoStatsRepository.findByRepositoryFullName(repoFullName)
                    .orElse(new RepositoryStats());

            // Update fields
            stats.setRepositoryFullName(repoFullName);
            stats.setTotalCommits(totalCommits);
            stats.setContributorCount(contributorCount);
            stats.setLastUpdatedAt(LocalDateTime.now());

            // Optionally serialize weekly activity if needed
            // ObjectMapper objectMapper = new ObjectMapper();
            // stats.setWeeklyCommitActivityJson(objectMapper.writeValueAsString(commitActivity.getWeeks()));

            repoStatsRepository.save(stats);
            log.debug("Successfully saved/updated stats for {}", repoFullName);

        } catch (Exception e) { // Catch broader exceptions during DB interaction
            log.error("Failed to save stats for {} to database: {}", repoFullName, e.getMessage(), e);
            // Consider handling DataIntegrityViolationException etc.
        }

    // Add these methods to GheStatsService.java

    private static final int MAX_STATS_RETRIES = 5;
    private static final long STATS_RETRY_DELAY_MS = 3000; // 3 seconds

    private GHCommitActivityStats fetchCommitActivityWithRetry(GHRepository repo) throws IOException, InterruptedException {
        for (int i = 0; i < MAX_STATS_RETRIES; i++) {
            try {
                log.trace("Attempt {} to fetch commit activity for {}", i + 1, repo.getFullName());
                return repo.getCommitActivity(); // This might throw HttpException on 202/404
            } catch (HttpException e) {
                if (e.getResponseCode() == 202) { // 202 Accepted
                    log.info("Commit activity for {} not ready (202 Accepted). Retrying in {}ms...", repo.getFullName(), STATS_RETRY_DELAY_MS);
                    Thread.sleep(STATS_RETRY_DELAY_MS);
                } else if (e.getResponseCode() == 404) {
                    log.warn("Commit activity for {} returned 404 (Not Found - possibly empty repo or stats disabled).", repo.getFullName());
                    return null; // Treat as no activity available
                } else {
                    throw e; // Re-throw other HTTP errors
                }
            }
        }
        log.error("Failed to get commit activity for {} after {} retries.", repo.getFullName(), MAX_STATS_RETRIES);
        return null; // Indicate failure after retries
    }

    private List<GHContributorStats> fetchContributorStatsWithRetry(GHRepository repo) throws IOException, InterruptedException {
        for (int i = 0; i < MAX_STATS_RETRIES; i++) {
            try {
                log.trace("Attempt {} to fetch contributor stats for {}", i + 1, repo.getFullName());
                // The getContributorStats might return null or empty list directly,
                // or throw HttpException on 202/404 depending on library version/implementation.
                // Let's assume it follows similar pattern to commit activity for robustness.
                return repo.getContributorStats();
            } catch (HttpException e) {
                if (e.getResponseCode() == 202) { // 202 Accepted
                    log.info("Contributor stats for {} not ready (202 Accepted). Retrying in {}ms...", repo.getFullName(), STATS_RETRY_DELAY_MS);
                    Thread.sleep(STATS_RETRY_DELAY_MS);
                } else if (e.getResponseCode() == 404) {
                    log.warn("Contributor stats for {} returned 404 (Not Found - possibly empty repo or stats disabled).", repo.getFullName());
                    return null; // Treat as no contributors/stats available
                } else {
                    throw e; // Re-throw other HTTP errors
                }
            }
        }
        log.error("Failed to get contributor stats for {} after {} retries.", repo.getFullName(), MAX_STATS_RETRIES);
        return null; // Indicate failure after retries
    }
}