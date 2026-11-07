package com.yhcanbay.opsbridge.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.yhcanbay.opsbridge.dto.request.ProjectRequest;
import com.yhcanbay.opsbridge.dto.response.ProjectResponse;

import jakarta.validation.Valid;

public interface IProjectController {

    @GetMapping
    List<ProjectResponse> getAllProjects();

    @GetMapping("/{id}")
    ProjectResponse getProjectById(@PathVariable(name = "id") UUID projectId);

    @PostMapping
    ResponseEntity<ProjectResponse> saveProject(@Valid @RequestBody ProjectRequest projectRequest);

    @PutMapping("/{id}")
    ProjectResponse updateProject(@PathVariable(name = "id") UUID projectId,
            @Valid @RequestBody ProjectRequest projectRequest);

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteProject(@PathVariable(name = "id") UUID projectId);
}
