package com.yhcanbay.opsbridge.controller.impl;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yhcanbay.opsbridge.controller.IProjectController;
import com.yhcanbay.opsbridge.dto.request.ProjectRequest;
import com.yhcanbay.opsbridge.dto.response.ProjectResponse;
import com.yhcanbay.opsbridge.service.IProjectService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/projects")
@RequiredArgsConstructor
public class ProjectControllerImpl implements IProjectController {

    private final IProjectService projectService;

    @Override
    public List<ProjectResponse> getAllProjects() {
        return projectService.getAllProjects();
    }

    @Override
    public ProjectResponse getProjectById(UUID projectId) {
        return projectService.getProjectById(projectId);
    }

    @Override
    public ResponseEntity<ProjectResponse> saveProject(ProjectRequest projectRequest) {
        ProjectResponse savedProject = projectService.saveProject(projectRequest);
        URI location = URI.create("/api/projects/" + savedProject.getId());
        return ResponseEntity.created(location).body(savedProject);
    }

    @Override
    public ProjectResponse updateProject(UUID projectId, ProjectRequest projectRequest) {
        return projectService.updateProject(projectId, projectRequest);
    }

    @Override
    public ResponseEntity<Void> deleteProject(UUID projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }
}
