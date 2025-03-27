package com.example.githubstats.repository;

import com.example.githubstats.entity.CommitStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommitStatsRepository extends JpaRepository<CommitStats,Long> {
    Optional<CommitStats> findByRepoNameAndSha(String repoName, String sha);
}
