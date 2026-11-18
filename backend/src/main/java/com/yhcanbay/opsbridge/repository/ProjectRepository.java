package com.yhcanbay.opsbridge.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.yhcanbay.opsbridge.entity.ProjectProfile;

public interface ProjectRepository extends JpaRepository<ProjectProfile, UUID> {

    boolean existsByGithubConnectionId(UUID connectionId);

    @EntityGraph(attributePaths = "capabilities")
    List<ProjectProfile> findAllByOrderByNameAsc();

    @Override
    @EntityGraph(attributePaths = "capabilities")
    Optional<ProjectProfile> findById(UUID id);
}
