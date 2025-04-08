package com.example.githubstats.dto.bitbucketdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDCHunk {
    // Fields like source/destination line numbers, context string might be here
    private List<BitbucketDCSegment> segments;

    public List<BitbucketDCSegment> getSegments() { return segments; }
    public void setSegments(List<BitbucketDCSegment> segments) { this.segments = segments; }
}