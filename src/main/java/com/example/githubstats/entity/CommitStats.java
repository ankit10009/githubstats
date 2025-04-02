package com.example.githubstats.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "commit_stats", indexes = {
        // Index updated to include source
        @Index(name = "idx_source_repo_sha", columnList = "source, repoName, sha", unique = true)
})
public class CommitStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20) // Added source column
    private String source; // e.g., "GitHub", "BitbucketCloud"

    @Column(nullable = false) // Stores full name (org/repo) or (workspace/repo)
    private String repoName;

    @Column(nullable = false, length = 64) // Increased length for potentially longer Bitbucket SHAs
    private String sha;

    private String authorName;
    private String authorEmail;
    private LocalDateTime commitDate;

    // Allow nulls for Bitbucket as diffstat is complex
    private Integer linesAdded;
    private Integer linesRemoved;
    private Integer filesChanged; // Files changed count

    public CommitStats() {
    }

    // Updated constructor
    public CommitStats(String source, String repoName, String sha, String authorName, String authorEmail,
            LocalDateTime commitDate, Integer linesAdded, Integer linesRemoved, Integer filesChanged) {
        this.source = source;
        this.repoName = repoName;
        this.sha = sha;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.commitDate = commitDate;
        this.linesAdded = linesAdded;
        this.linesRemoved = linesRemoved;
        this.filesChanged = filesChanged;
    }

    // --- Getters and Setters for all fields (including source) ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public LocalDateTime getCommitDate() {
        return commitDate;
    }

    public void setCommitDate(LocalDateTime commitDate) {
        this.commitDate = commitDate;
    }

    public Integer getLinesAdded() {
        return linesAdded;
    } // Return Integer

    public void setLinesAdded(Integer linesAdded) {
        this.linesAdded = linesAdded;
    } // Accept Integer

    public Integer getLinesRemoved() {
        return linesRemoved;
    } // Return Integer

    public void setLinesRemoved(Integer linesRemoved) {
        this.linesRemoved = linesRemoved;
    } // Accept Integer

    public Integer getFilesChanged() {
        return filesChanged;
    } // Return Integer

    public void setFilesChanged(Integer filesChanged) {
        this.filesChanged = filesChanged;
    } // Accept Integer
}