package com.example.githubstats.entity;

import jakarta.persistence.*; // Correct import
import java.time.LocalDateTime;

@Entity
@Table(name = "repository_filter_control", indexes = {
        @Index(name = "idx_source", columnList = "source")
})
// Use a composite key or a generated ID if filterCriteria + source isn't unique
// enough
@IdClass(RepositoryFilterControlId.class) // Using @IdClass for composite key
public class RepositoryFilterControl {

    @Id // Part of composite key
    @Column(length = 20)
    private String source; // "GitHub", "BitbucketCloud"

    @Id // Part of composite key
    @Column(name = "filter_criteria", length = 255)
    private String filterCriteria; // Filter text (repo name substring, project key, etc.)

    @Column(name = "last_fetch_timestamp")
    private LocalDateTime lastFetchTimestamp;

    public RepositoryFilterControl() {
    }

    public RepositoryFilterControl(String source, String filterCriteria, LocalDateTime lastFetchTimestamp) {
        this.source = source;
        this.filterCriteria = filterCriteria;
        this.lastFetchTimestamp = lastFetchTimestamp;
    }

    // --- Getters and Setters ---
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(String filterCriteria) {
        this.filterCriteria = filterCriteria;
    }

    public LocalDateTime getLastFetchTimestamp() {
        return lastFetchTimestamp;
    }

    public void setLastFetchTimestamp(LocalDateTime lastFetchTimestamp) {
        this.lastFetchTimestamp = lastFetchTimestamp;
    }
}