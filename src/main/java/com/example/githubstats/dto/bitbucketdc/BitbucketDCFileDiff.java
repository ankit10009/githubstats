package com.example.githubstats.dto.bitbucketdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty; // Import if needed for mapping
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDCFileDiff {
    // Example fields - adjust based on your actual JSON response
    private String source; // Path object or string for old path
    private String destination; // Path object or string for new path
    private List<BitbucketDCHunk> hunks;
    // Add type (ADD, MODIFY), binary flag etc. if needed

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public List<BitbucketDCHunk> getHunks() { return hunks; }
    public void setHunks(List<BitbucketDCHunk> hunks) { this.hunks = hunks; }
}