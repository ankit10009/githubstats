package com.example.githubstats.config;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import java.io.IOException;

@Configuration
public class GitHubConfig {

    private static final Logger log = LoggerFactory.getLogger(GitHubConfig.class);

    @Value("${github.token}")
    private String githubToken;

    // Use @Nullable or Optional if GHE URL might not be present
    @Value("${github.enterprise.url:#{null}}") // Default to null if property not set
    @Nullable
    private String enterpriseUrl;

    @Value("${github.enabled:false}") // Default to false if not set
    private boolean githubEnabled;

    @Bean
    public GitHub gitHub() throws IOException {
        if (!githubEnabled) {
            log.warn(
                    "GitHub integration is disabled via configuration (github.enabled=false). Returning null GitHub client.");
            return null; // Or throw an exception if preferred
        }
        if (githubToken == null || githubToken.isBlank()) {
            log.error("GitHub token (github.token) is not configured. Cannot create GitHub client.");
            // Depending on requirements, either return null or throw an exception
            // throw new IllegalStateException("GitHub token is required but not
            // configured.");
            return null; // Allows application to start, but GitHubService will fail later if used
        }

        GitHubBuilder builder = new GitHubBuilder().withOAuthToken(githubToken);

        if (enterpriseUrl != null && !enterpriseUrl.isBlank()) {
            log.info("Connecting to GitHub Enterprise at: {}", enterpriseUrl);
            builder.withEndpoint(enterpriseUrl);
        } else {
            log.info("Connecting to GitHub.com");
        }
        try {
            return builder.build();
        } catch (IOException e) {
            log.error("Failed to build GitHub client: {}", e.getMessage(), e);
            throw e; // Re-throw or handle appropriately
        }
    }
}