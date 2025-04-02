package com.example.githubstats.entity;

import java.io.Serializable;
import java.util.Objects;

// Class for the composite primary key
public class RepositoryFilterControlId implements Serializable {
    private String source;
    private String filterCriteria;

    public RepositoryFilterControlId() {
    }

    public RepositoryFilterControlId(String source, String filterCriteria) {
        this.source = source;
        this.filterCriteria = filterCriteria;
    }

    // Getters, Setters, hashCode, equals are crucial for composite keys
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(String filterCriteria) {
        this.filterCriteria = filterCriteria;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RepositoryFilterControlId that = (RepositoryFilterControlId) o;
        return Objects.equals(source, that.source) && Objects.equals(filterCriteria, that.filterCriteria);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, filterCriteria);
    }
}