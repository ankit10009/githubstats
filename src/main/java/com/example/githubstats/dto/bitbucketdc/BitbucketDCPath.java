package com.example.githubstats.dto.bitbucketdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDCPath {
    // The API often returns a 'toString' field with the actual path
    @JsonProperty("toString")
    private String pathString;

    // Getters & Setters
    public String getPathString() { return pathString; }
    public void setPathString(String pathString) { this.pathString = pathString; }
}