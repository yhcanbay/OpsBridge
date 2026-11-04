package com.yhcanbay.opsbridge.service;

import com.yhcanbay.opsbridge.dto.request.ProjectRequest;
import com.yhcanbay.opsbridge.dto.response.ProjectResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.exception.ProjectNotFoundException;
import com.yhcanbay.opsbridge.mapper.ProjectMapper;
import com.yhcanbay.opsbridge.repository.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    public ProjectService(ProjectRepository projectRepository, ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
    }

    public List<ProjectResponse> list() {
        return projectRepository.findAllByOrderByNameAsc().stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    public ProjectResponse get(UUID projectId) {
        ProjectProfile project = findProject(projectId);
        return projectMapper.toResponse(project);
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        ProjectProfile project = projectMapper.toEntity(request);
        ProjectProfile savedProject = projectRepository.saveAndFlush(project);
        return projectMapper.toResponse(savedProject);
    }

    @Transactional
    public ProjectResponse update(UUID projectId, ProjectRequest request) {
        ProjectProfile project = findProject(projectId);
        projectMapper.updateEntity(request, project);

        // Flush, lifecycle callback'lerinin updatedAt alanını DTO'dan önce yenilemesini sağlar.
        ProjectProfile savedProject = projectRepository.saveAndFlush(project);
        return projectMapper.toResponse(savedProject);
    }

    @Transactional
    public void delete(UUID projectId) {
        ProjectProfile project = findProject(projectId);
        projectRepository.delete(project);
    }

    private ProjectProfile findProject(UUID projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }
}
