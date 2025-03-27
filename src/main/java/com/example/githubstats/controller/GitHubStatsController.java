package com.example.githubstats.controller;

import com.example.githubstats.service.GitHubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/github")
public class GitHubStatsController {
    @Autowired
    private GitHubService gitHubService;

    @PostMapping("/fetch/{username}")
    public ResponseEntity<String> fetchStats(@PathVariable String username) throws IOException {

        // Consider running this asynchronously (@Async) for long-running tasks
        try {
            gitHubService.fetchAndSaveStatsForUser(username);
            return ResponseEntity.ok("GitHub fetch process started for user: " + username);
        } catch (IOException e) {
            // Log the exception properly
            return ResponseEntity.internalServerError().body("Failed to start fetch process: " + e.getMessage());
        }
    }
}
