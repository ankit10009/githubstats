package com.example.githubstats.dto.bitbucketdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// Represents the overall JSON response from the /diff endpoint
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDCDiffResponse {
    // Assuming the top level contains a list named 'diffs'
    // Adjust if the structure is different (e.g., pagination info, single diff object)
    private List<BitbucketDCFileDiff> diffs;
    // Add pagination fields if the diff response itself is paginated

    public List<BitbucketDCFileDiff> getDiffs() { return diffs; }
    public void setDiffs(List<BitbucketDCFileDiff> diffs) { this.diffs = diffs; }
}