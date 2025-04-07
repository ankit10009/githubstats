package com.example.githubstats.dto.bitbucketdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDCCommit {
    private String id; // Commit SHA
    private String displayId; // Short SHA
    private BitbucketDCAuthor author; // Author info
    private long authorTimestamp; // Date as epoch milliseconds
    private String message;
    private List<BitbucketDCParentInfo> parents;

    // Helper method to check for merge commit
    public boolean isMergeCommit() {
        return this.parents != null && this.parents.size() > 1;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisplayId() { return displayId; }
    public void setDisplayId(String displayId) { this.displayId = displayId; }
    public BitbucketDCAuthor getAuthor() { return author; }
    public void setAuthor(BitbucketDCAuthor author) { this.author = author; }
    public long getAuthorTimestamp() { return authorTimestamp; }
    public void setAuthorTimestamp(long authorTimestamp) { this.authorTimestamp = authorTimestamp; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<BitbucketDCParentInfo> getParents() { return parents; }
    public void setParents(List<BitbucketDCParentInfo> parents) { this.parents = parents; }
}