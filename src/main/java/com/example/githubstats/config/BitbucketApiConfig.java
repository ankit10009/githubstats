package com.example.githubstats.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BitbucketApiConfig {
    private static final Logger log = LoggerFactory.getLogger(BitbucketApiConfig.class);

    @Value("${bitbucket.base.url:#{null}}")
    private String bitbucketBaseUrl;

    @Value("${bitbucket.auth.token:#{null}}") // Personal Access Token
    private String personalAccessToken;

    @Value("${bitbucket.enabled:false}")
    private boolean bitbucketEnabled;

    @Bean("bitbucketRestTemplate")
    public RestTemplate bitbucketRestTemplate(RestTemplateBuilder builder) {
        if (!bitbucketEnabled) {
            log.warn("Bitbucket Data Center integration is disabled (bitbucket.enabled=false). Returning null RestTemplate.");
            return null;
        }
        if (bitbucketBaseUrl == null || bitbucketBaseUrl.isBlank() || personalAccessToken == null || personalAccessToken.isBlank()) {
            log.warn("Bitbucket Data Center configuration (base URL, auth token) is incomplete. Returning null RestTemplate.");
            return null;
        }

        // Base URL should likely include /rest/api/latest or /rest/api/1.0 depending on API version target
        // Let's assume '/rest/api/latest' is standard for newer DCs
        String rootUri = bitbucketBaseUrl.endsWith("/") ? bitbucketBaseUrl + "rest/api/latest" : bitbucketBaseUrl + "/rest/api/latest";
        log.info("Configuring Bitbucket DC RestTemplate for root URI: {}", rootUri);

        return builder
                .rootUri(rootUri)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + personalAccessToken) // Use Bearer token auth
                .build();
    }
}