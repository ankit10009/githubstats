package com.example.githubstats.dto.bitbucketdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDCChangeEntry {
    private BitbucketDCPath path; // Contains toString() which is the path
    private String type; // MODIFY, ADD, DELETE, etc.
    private int linesAdded;
    private int linesRemoved;
    // Add other fields if needed (e.g., executable bit changes)

    // Getters & Setters
    public BitbucketDCPath getPath() { return path; }
    public void setPath(BitbucketDCPath path) { this.path = path; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getLinesAdded() { return linesAdded; }
    public void setLinesAdded(int linesAdded) { this.linesAdded = linesAdded; }
    public int getLinesRemoved() { return linesRemoved; }
    public void setLinesRemoved(int linesRemoved) { this.linesRemoved = linesRemoved; }
}