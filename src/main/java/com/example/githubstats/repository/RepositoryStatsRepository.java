package com.example.githubstats.repository;

import com.example.githubstats.entity.RepositoryStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryStatsRepository extends JpaRepository<RepositoryStats, Long> {
    // Find existing record to update it
    Optional<RepositoryStats> findByRepositoryFullName(String repositoryFullName);
}