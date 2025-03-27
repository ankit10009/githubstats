package com.example.githubstats;

import com.example.githubstats.service.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GithubstatsApplication {

	private static final Logger log = LoggerFactory.getLogger(GithubstatsApplication.class);

	@Autowired
	private GitHubService gitHubService;

	public static void main(String[] args) {
		SpringApplication.run(GithubstatsApplication.class, args);
	}

	@Bean
	CommandLineRunner runner(){
		return args -> {
			log.info("Application started. Triggering GitHub fetch...");
			String githubUsername = "ankit10009"; // Example: Fetch stats for GitHub's octocat
			gitHubService.fetchAndSaveStatsForUser(githubUsername);
			log.info("GitHub fetch process initiated.");
		};
	}

}
