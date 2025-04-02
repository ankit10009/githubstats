package com.example.githubstats.repository;

import com.example.githubstats.entity.RepositoryFilterControl;
import com.example.githubstats.entity.RepositoryFilterControlId; // Import the ID class
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
// Use the composite ID class here
public interface RepositoryFilterControlRepository
        extends JpaRepository<RepositoryFilterControl, RepositoryFilterControlId> {
    // Find all filters for a specific source (e.g., "GitHub", "BitbucketCloud")
    List<RepositoryFilterControl> findBySource(String source);
}