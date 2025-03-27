package com.example.githubstats.config;


import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;


@Configuration
public class GitHubConfig {

    private static final Logger log = LoggerFactory.getLogger(GitHubConfig.class);

    @Value("${github.token}")
    private String githubToken;

    @Bean
    public GitHub gitHub() throws IOException {
        return new GitHubBuilder().withOAuthToken(githubToken).build();
    }

}
