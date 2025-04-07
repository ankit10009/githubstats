package com.example.githubstats.dto.bitbucketdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
// Reuse the generic paged response for /changes endpoint
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDCChangesResponse extends BitbucketDCPagedResponse<BitbucketDCChangeEntry> {
    // Inherits pagination fields, only contains List<BitbucketDCChangeEntry> values
}