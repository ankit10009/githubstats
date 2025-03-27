package com.example.githubstats.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "commit_stats",indexes = {
        @Index(name = "idx_repo_sha",columnList = "repoName, sha", unique = true)
})
public class CommitStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String repoName;

    @Column(nullable = false,length = 40)
    private String sha;

    private String authorName;
    private String authorEmail;
    private LocalDateTime commitDate;

    private int linesAdded;
    private int linesRemoved;
    private int totalChanges;

    public CommitStats() {
    }

    public CommitStats(String repoName, String sha, String authorName, String authorEmail, LocalDateTime commitDate, int linesAdded, int linesRemoved, int totalChanges) {
        this.repoName = repoName;
        this.sha = sha;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.commitDate = commitDate;
        this.linesAdded = linesAdded;
        this.linesRemoved = linesRemoved;
        this.totalChanges = totalChanges;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getLinesAdded() {
        return linesAdded;
    }

    public void setLinesAdded(int linesAdded) {
        this.linesAdded = linesAdded;
    }

    public int getLinesRemoved() {
        return linesRemoved;
    }

    public void setLinesRemoved(int linesRemoved) {
        this.linesRemoved = linesRemoved;
    }

    public int getTotalChanges() {
        return totalChanges;
    }

    public void setTotalChanges(int totalChanges) {
        this.totalChanges = totalChanges;
    }
}
