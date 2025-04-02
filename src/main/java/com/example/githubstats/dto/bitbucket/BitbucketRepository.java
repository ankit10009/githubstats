package com.example.githubstats.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Simplified Repository DTO - Adjust fields as needed
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketRepository {
    private String slug;
    private String name;
    @JsonProperty("full_name") // Example mapping
    private String fullName;
    private BitbucketProject project; // Nested project info

    // Getters and Setters
    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public BitbucketProject getProject() {
        return project;
    }

    public void setProject(BitbucketProject project) {
        this.project = project;
    }
}