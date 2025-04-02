package com.example.githubstats.dto.bitbucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Represents the 'author' object within a Bitbucket commit
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketAuthor {
    private String raw; // Raw author string like "Name <email@example.com>"
    private BitbucketUser user; // Linked Bitbucket user, if available

    // Getters and Setters
    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public BitbucketUser getUser() {
        return user;
    }

    public void setUser(BitbucketUser user) {
        this.user = user;
    }

    // Helper method to attempt parsing name/email (basic)
    public String getParsedName() {
        if (raw != null && raw.contains("<")) {
            return raw.substring(0, raw.indexOf('<')).trim();
        } else if (user != null && user.getDisplayName() != null) {
            return user.getDisplayName();
        }
        return raw; // Fallback
    }

    public String getParsedEmail() {
        if (raw != null && raw.contains("<") && raw.contains(">")) {
            return raw.substring(raw.indexOf('<') + 1, raw.indexOf('>')).trim();
        }
        // Bitbucket user object usually doesn't expose email directly via API for
        // privacy
        return null;
    }
}