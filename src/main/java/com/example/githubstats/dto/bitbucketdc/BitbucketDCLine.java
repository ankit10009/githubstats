package com.example.githubstats.dto.bitbucketdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Represents a single line object within a segment's 'lines' list
// Often contains the line content, but for counting we might only need its presence
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDCLine {
    // Example field, adjust if needed (e.g., "content": "...")
    private String line    ;
    // Add other fields if present in your JSON, like line numbers, etc.

    public String getContent() { return line; }
    public void setContent(String line) { this.line = line; }
}