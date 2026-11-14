package com.yhcanbay.opsbridge.mapper;

import org.springframework.stereotype.Component;

import com.yhcanbay.opsbridge.dto.request.ProjectRequest;
import com.yhcanbay.opsbridge.dto.response.ProjectResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;

@Component
public class ProjectMapper {

    public ProjectProfile createEntity(ProjectRequest request) {
        ProjectProfile project = new ProjectProfile();
        updateEntity(request, project);
        return project;
    }

    public void updateEntity(ProjectRequest request, ProjectProfile project) {
        project.setName(request.getName().trim());
        project.setRepositoryUrl(request.getRepositoryUrl().trim());
        project.setGithubConnectionId(request.getGithubConnectionId());
        project.setEnvironment(request.getEnvironment().trim());
        project.setJenkinsBaseUrlRef(normalizeOptionalText(request.getJenkinsBaseUrlRef()));
        project.setJenkinsJobPath(normalizeOptionalText(request.getJenkinsJobPath()));
        project.setContainerLabelSelector(normalizeOptionalText(request.getContainerLabelSelector()));
        project.setHealthUrl(normalizeOptionalText(request.getHealthUrl()));
        project.setCapabilities(request.getCapabilities());
        project.setEnabled(request.getEnabled());
    }

    public ProjectResponse toDto(ProjectProfile project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .repositoryUrl(project.getRepositoryUrl())
                .githubConnectionId(project.getGithubConnectionId())
                .environment(project.getEnvironment())
                .jenkinsBaseUrlRef(project.getJenkinsBaseUrlRef())
                .jenkinsJobPath(project.getJenkinsJobPath())
                .containerLabelSelector(project.getContainerLabelSelector())
                .healthUrl(project.getHealthUrl())
                .capabilities(project.getCapabilities())
                .enabled(project.isEnabled())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
