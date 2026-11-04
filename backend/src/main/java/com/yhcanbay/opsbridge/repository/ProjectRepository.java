package com.yhcanbay.opsbridge.repository;

import com.yhcanbay.opsbridge.entity.ProjectProfile;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<ProjectProfile, UUID> {

    @EntityGraph(attributePaths = "capabilities")
    List<ProjectProfile> findAllByOrderByNameAsc();

    @Override
    @EntityGraph(attributePaths = "capabilities")
    Optional<ProjectProfile> findById(UUID id);
}
