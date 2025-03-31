package com.example.githubstats;

// Other imports...
import com.example.githubstats.service.FetchOrchestrationService;
import com.example.githubstats.service.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync // Enable Spring's asynchronous method execution capability
@EnableScheduling
public class GithubstatsApplication {

	private static final Logger log = LoggerFactory.getLogger(GithubstatsApplication.class);

	// Keep the constructor injection for services needed by CommandLineRunner if still used
	private final FetchOrchestrationService fetchOrchestrationService; // Inject Orchestrator

	public GithubstatsApplication(FetchOrchestrationService fetchOrchestrationService) {
		this.fetchOrchestrationService = fetchOrchestrationService;
	}


	public static void main(String[] args) {
		SpringApplication.run(GithubstatsApplication.class, args);
	}

	// Optional: Keep CommandLineRunner to trigger on startup, or remove if only triggered by API
	@Bean
	CommandLineRunner runner() {
		return args -> {
			log.info("Application started. Triggering initial fetch via CommandLineRunner...");
			// Delegate the work to the orchestration service
			fetchOrchestrationService.triggerAllFilterProcessing();
			log.info("CommandLineRunner finished triggering asynchronous fetch.");
		};
	}
}