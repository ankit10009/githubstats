package com.example.githubstats.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "repository_stats", indexes = {
        @Index(name = "idx_repo_name", columnList = "repositoryFullName", unique = true)
})
@Data
@NoArgsConstructor
public class RepositoryStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String repositoryFullName; // e.g., "ORG_NAME/repo-name"

    private Integer totalCommits;
    private Integer contributorCount;

    // Store weekly activity as JSON? Or skip for simplicity?
    // @Column(columnDefinition = "TEXT") // Or "JSONB" if using specific Postgres JSON type
    // private String weeklyCommitActivityJson;

    private LocalDateTime lastUpdatedAt;

    public RepositoryStats(String repositoryFullName, Integer totalCommits, Integer contributorCount) {
        this.repositoryFullName = repositoryFullName;
        this.totalCommits = totalCommits;
        this.contributorCount = contributorCount;
    }
}