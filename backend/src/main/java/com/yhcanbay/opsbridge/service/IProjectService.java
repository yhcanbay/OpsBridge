package com.yhcanbay.opsbridge.service;

import java.util.List;
import java.util.UUID;

import com.yhcanbay.opsbridge.dto.request.ProjectRequest;
import com.yhcanbay.opsbridge.dto.response.ProjectResponse;

public interface IProjectService {

    List<ProjectResponse> getAllProjects();

    ProjectResponse getProjectById(UUID projectId);

    ProjectResponse saveProject(ProjectRequest projectRequest);

    ProjectResponse updateProject(UUID projectId, ProjectRequest projectRequest);

    void deleteProject(UUID projectId);
}
