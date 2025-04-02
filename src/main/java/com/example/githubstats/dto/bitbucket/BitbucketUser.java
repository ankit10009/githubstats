package com.example.githubstats.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Simplified Bitbucket User DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketUser {
    @JsonProperty("display_name")
    private String displayName;
    private String uuid; // User's unique ID
    private String nickname; // Often the username

    // Getters and Setters
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}