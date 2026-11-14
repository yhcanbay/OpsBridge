package com.yhcanbay.opsbridge.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yhcanbay.opsbridge.dto.request.ProjectRequest;
import com.yhcanbay.opsbridge.dto.response.ProjectResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.exception.ProjectNotFoundException;
import com.yhcanbay.opsbridge.mapper.ProjectMapper;
import com.yhcanbay.opsbridge.repository.ProjectRepository;
import com.yhcanbay.opsbridge.service.IGitService;
import com.yhcanbay.opsbridge.service.IProjectService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectServiceImpl implements IProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final IGitService gitService;

    @Override
    public List<ProjectResponse> getAllProjects() {
        List<ProjectProfile> projectList = projectRepository.findAllByOrderByNameAsc();
        List<ProjectResponse> dtoList = new ArrayList<>();

        for (ProjectProfile project : projectList) {
            ProjectResponse dtoProject = projectMapper.toDto(project);
            dtoList.add(dtoProject);
        }

        return dtoList;
    }

    @Override
    public ProjectResponse getProjectById(UUID projectId) {
        ProjectProfile project = findProjectById(projectId);
        return projectMapper.toDto(project);
    }

    @Override
    @Transactional
    public ProjectResponse saveProject(ProjectRequest projectRequest) {
        if (projectRequest.getGithubConnectionId() != null) {
            gitService.validateRepositoryAccess(
                    projectRequest.getRepositoryUrl().trim(), projectRequest.getGithubConnectionId());
        }
        ProjectProfile project = projectMapper.createEntity(projectRequest);
        ProjectProfile savedProject = projectRepository.saveAndFlush(project);
        return projectMapper.toDto(savedProject);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(UUID projectId, ProjectRequest projectRequest) {
        ProjectProfile project = findProjectById(projectId);
        boolean repositoryChanged = !project.getRepositoryUrl().equals(projectRequest.getRepositoryUrl().trim());
        boolean connectionChanged = !Objects.equals(
                project.getGithubConnectionId(), projectRequest.getGithubConnectionId());
        if (projectRequest.getGithubConnectionId() != null && (repositoryChanged || connectionChanged)) {
            gitService.validateRepositoryAccess(
                    projectRequest.getRepositoryUrl().trim(), projectRequest.getGithubConnectionId());
        }
        projectMapper.updateEntity(projectRequest, project);

        // updatedAt, yanıt oluşturulmadan önce veritabanına yazılırken yenilenir.
        ProjectProfile updatedProject = projectRepository.saveAndFlush(project);
        return projectMapper.toDto(updatedProject);
    }

    @Override
    @Transactional
    public void deleteProject(UUID projectId) {
        ProjectProfile project = findProjectById(projectId);
        projectRepository.delete(project);
    }

    private ProjectProfile findProjectById(UUID projectId) {
        Optional<ProjectProfile> optionalProject = projectRepository.findById(projectId);

        if (optionalProject.isEmpty()) {
            throw new ProjectNotFoundException(projectId);
        }

        return optionalProject.get();
    }
}
