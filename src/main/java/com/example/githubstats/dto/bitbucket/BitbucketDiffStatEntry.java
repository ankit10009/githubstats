package com.example.githubstats.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDiffStatEntry {

    private String status; // "added", "removed", "modified", "renamed"
    @JsonProperty("lines_added")
    private int linesAdded;
    @JsonProperty("lines_removed")
    private int linesRemoved;
    @JsonProperty("old_path") // Present for renamed/moved
    private String oldPath;
    @JsonProperty("new_path") // Present for renamed/moved
    private String newPath;
    private String path; // Path of the file

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getLinesAdded() { return linesAdded; }
    public void setLinesAdded(int linesAdded) { this.linesAdded = linesAdded; }
    public int getLinesRemoved() { return linesRemoved; }
    public void setLinesRemoved(int linesRemoved) { this.linesRemoved = linesRemoved; }
    public String getOldPath() { return oldPath; }
    public void setOldPath(String oldPath) { this.oldPath = oldPath; }
    public String getNewPath() { return newPath; }
    public void setNewPath(String newPath) { this.newPath = newPath; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}


