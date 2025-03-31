package com.example.githubstats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name="github_repo_selection")
public class RepositoryFilterControl {

    @Id
    @Column(name="csi_id",length = 255)  // Primary key is the CSI ID
    private String filterCriteria;

    @Column(name="last_fetch_timestamp")
    private LocalDateTime lastFetchTimestamp; // Timestamp specific to this filter

    public RepositoryFilterControl() {
    }

    public RepositoryFilterControl(String filterCriteria, LocalDateTime lastFetchTimestamp) {
        this.filterCriteria = filterCriteria;
        this.lastFetchTimestamp = lastFetchTimestamp;
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
