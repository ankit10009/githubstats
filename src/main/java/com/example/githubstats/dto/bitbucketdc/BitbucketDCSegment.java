package com.example.githubstats.dto.bitbucketdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDCSegment {
    private String type; // "ADDED", "REMOVED", "CONTEXT"
    private List<BitbucketDCLine> lines;
    // Add other segment fields if present (e.g., truncated)

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<BitbucketDCLine> getLines() { return lines; }
    public void setLines(List<BitbucketDCLine> lines) { this.lines = lines; }
}