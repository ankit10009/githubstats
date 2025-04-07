package com.example.githubstats.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
// Reuse the generic paged response structure if diffstat is paginated similarly
// If not paginated, create a simpler DTO just holding List<BitbucketDiffStatEntry>

// Assuming diffstat uses the same pagination structure
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDiffStatResponse extends BitbucketPagedResponse<BitbucketDiffStatEntry> {
    // Inherits 'values', 'next', 'page', 'pagelen', 'size' from parent
    // No additional fields needed if structure matches
}