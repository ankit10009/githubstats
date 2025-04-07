package com.example.githubstats.dto.bitbucketdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDCParentInfo {
    private String id; // SHA of the parent commit
    private String displayId; // Short SHA

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisplayId() { return displayId; }
    public void setDisplayId(String displayId) { this.displayId = displayId; }
}