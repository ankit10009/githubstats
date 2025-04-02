package com.example.githubstats;

import com.example.githubstats.service.FetchOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync // Enable asynchronous execution
@EnableScheduling // Enable scheduled tasks
public class GithubstatsApplication {

	private static final Logger log = LoggerFactory.getLogger(GithubstatsApplication.class);

	// Inject orchestrator if needed for CommandLineRunner trigger
	private final FetchOrchestrationService fetchOrchestrationService;

	public GithubstatsApplication(FetchOrchestrationService fetchOrchestrationService) {
		this.fetchOrchestrationService = fetchOrchestrationService;
	}

	public static void main(String[] args) {
		SpringApplication.run(GithubstatsApplication.class, args);
	}

	// Optional: Trigger fetch on application startup
	@Bean
	CommandLineRunner runner() {
		return args -> {
			log.info("CommandLineRunner: Triggering initial fetch on startup...");
			// Trigger processing for all configured sources
			fetchOrchestrationService.triggerAllProcessing();
			log.info("CommandLineRunner: Asynchronous fetch trigger completed.");
		};
	}
}