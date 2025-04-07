package com.example.githubstats.dto.bitbucketdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDCProject {
    private String key;
    // Add other project fields if needed

    // Getters & Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}