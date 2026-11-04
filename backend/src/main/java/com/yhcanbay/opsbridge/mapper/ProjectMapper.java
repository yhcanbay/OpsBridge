package com.yhcanbay.opsbridge.mapper;

import com.yhcanbay.opsbridge.dto.request.ProjectRequest;
import com.yhcanbay.opsbridge.dto.response.ProjectResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;

import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public ProjectProfile toEntity(ProjectRequest request) {
        ProjectProfile project = new ProjectProfile();
        updateEntity(request, project);
        return project;
    }

    public void updateEntity(ProjectRequest request, ProjectProfile project) {
        project.setName(request.name().trim());
        project.setRepositoryUrl(request.repositoryUrl().trim());
        project.setEnvironment(request.environment().trim());
        project.setJenkinsBaseUrlRef(normalizeOptionalText(request.jenkinsBaseUrlRef()));
        project.setJenkinsJobPath(normalizeOptionalText(request.jenkinsJobPath()));
        project.setContainerLabelSelector(normalizeOptionalText(request.containerLabelSelector()));
        project.setHealthUrl(normalizeOptionalText(request.healthUrl()));
        project.setCapabilities(request.capabilities());
        project.setEnabled(request.enabled());
    }

    public ProjectResponse toResponse(ProjectProfile project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getRepositoryUrl(),
                project.getEnvironment(),
                project.getJenkinsBaseUrlRef(),
                project.getJenkinsJobPath(),
                project.getContainerLabelSelector(),
                project.getHealthUrl(),
                project.getCapabilities(),
                project.isEnabled(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
