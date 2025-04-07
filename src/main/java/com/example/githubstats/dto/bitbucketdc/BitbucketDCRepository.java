package com.example.githubstats.dto.bitbucketdc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitbucketDCRepository {
    private String slug;
    private String name;
    private BitbucketDCProject project; // Nested project info

    // Helper to get full name like PROJ/repo-slug
    public String getFullName() {
        return (project != null && project.getKey() != null ? project.getKey() + "/" : "") + slug;
    }

    // Getters & Setters
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BitbucketDCProject getProject() { return project; }
    public void setProject(BitbucketDCProject project) { this.project = project; }
}