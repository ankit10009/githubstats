package com.example.githubstats.repository;

import com.example.githubstats.entity.RepositoryFilterControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryFilterControlRepository extends JpaRepository<RepositoryFilterControl,String> {

}
