package com.example.githubstats.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "github_error_log")
public class ErrorLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 50)
    private String errorType; // e.g., "HTTP", "IO", "RUNTIME", "CONFIG"

    @Column(columnDefinition = "TEXT") // Use TEXT for potentially long messages
    private String errorMessage;

    private Integer httpStatusCode; // Nullable

    @Column(length = 500) // Context like Org, Repo, Commit SHA, Filter
    private String context;

    @Column(length = 255) // Link back to the filter being processed
    private String filterCriteria;

    public ErrorLog() {
        this.timestamp = LocalDateTime.now(); // Default to now
    }

    public ErrorLog(String errorType, String errorMessage, Integer httpStatusCode, String context, String filterCriteria) {
        this();
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.httpStatusCode = httpStatusCode;
        this.context = context;
        this.filterCriteria = filterCriteria;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(String filterCriteria) {
        this.filterCriteria = filterCriteria;
    }
}
