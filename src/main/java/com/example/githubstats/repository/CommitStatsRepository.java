package com.example.githubstats.repository;

import com.example.githubstats.entity.CommitStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommitStatsRepository extends JpaRepository<CommitStats, Long> {
    // Find by unique combination including source
    Optional<CommitStats> findBySourceAndRepoNameAndSha(String source, String repoName, String sha);
}