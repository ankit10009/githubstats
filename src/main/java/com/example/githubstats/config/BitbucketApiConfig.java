package com.example.githubstats.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BitbucketApiConfig {
    private static final Logger log = LoggerFactory.getLogger(BitbucketApiConfig.class);

    @Value("${bitbucket.base.url:#{null}}") // Default to null
    private String bitbucketBaseUrl;

    @Value("${bitbucket.auth.username:#{null}}") // Default to null
    private String username;

    @Value("${bitbucket.auth.app-password:#{null}}") // Default to null
    private String appPassword;

    @Value("${bitbucket.enabled:false}") // Default to false
    private boolean bitbucketEnabled;

    @Bean("bitbucketRestTemplate") // Qualify bean name
    public RestTemplate bitbucketRestTemplate(RestTemplateBuilder builder) {
        if (!bitbucketEnabled) {
            log.warn("Bitbucket integration is disabled (bitbucket.enabled=false). Returning null RestTemplate.");
            return null;
        }
        if (bitbucketBaseUrl == null || username == null || appPassword == null ||
                bitbucketBaseUrl.isBlank() || username.isBlank() || appPassword.isBlank()) {
            log.warn(
                    "Bitbucket configuration (base URL, username, app-password) is incomplete. Returning null RestTemplate.");
            // Depending on requirements, either return null or throw an exception
            return null;
        }

        log.info("Configuring Bitbucket RestTemplate for base URL: {}", bitbucketBaseUrl);
        return builder
                .rootUri(bitbucketBaseUrl)
                .basicAuthentication(username, appPassword) // Use Basic Auth with App Password
                .build();
    }
}