package com.example.githubstats.config;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Configuration
public class GitHubConfig {

    private static final Logger log = LoggerFactory.getLogger(GitHubConfig.class);

    @Value("${github.enterprise.url}")
    private String gheApiUrl;

    @Value("${github.enterprise.token}")
    private String gheToken;

    @Bean
    public GitHub gitHubEnterpriseClient() throws IOException {
        if (!StringUtils.hasText(gheToken)) {
            log.error("GitHub Enterprise token (github.enterprise.token) is not configured!");
            throw new IllegalStateException("GitHub Enterprise token is required.");
        }
        if (!StringUtils.hasText(gheApiUrl)) {
            log.error("GitHub Enterprise API URL (github.enterprise.url) is not configured!");
            throw new IllegalStateException("GitHub Enterprise API URL is required.");
        }

        log.info("Configuring GitHub client for Enterprise endpoint: {}", gheApiUrl);
        return new GitHubBuilder()
                .withEndpoint(gheApiUrl) // Set the GHE API endpoint
                .withOAuthToken(gheToken) // Use the GHE token
                .build();
    }
}