package com.example.githubstats.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime; // Bitbucket often uses OffsetDateTime

// Simplified Commit DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketCommit {
    private String hash; // The commit SHA
    private BitbucketAuthor author; // Contains raw author string and potentially user mapping
    private OffsetDateTime date; // Commit date
    private String message;
    private Integer parents; // Number of parents

    // Might need fields like 'rendered.message.raw' if message structure is complex

    // Getters and Setters
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public BitbucketAuthor getAuthor() {
        return author;
    }

    public void setAuthor(BitbucketAuthor author) {
        this.author = author;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getParents() {
        return parents;
    }

    public void setParents(Integer parents) {
        this.parents = parents;
    }
}