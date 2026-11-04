package com.yhcanbay.opsbridge.service;

import com.yhcanbay.opsbridge.dto.response.IntegrationStatusResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.exception.ProjectNotFoundException;
import com.yhcanbay.opsbridge.integration.adapter.HealthAdapter;
import com.yhcanbay.opsbridge.integration.adapter.JenkinsAdapter;
import com.yhcanbay.opsbridge.integration.adapter.RuntimeAdapter;
import com.yhcanbay.opsbridge.mapper.IntegrationMapper;
import com.yhcanbay.opsbridge.repository.ProjectRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class IntegrationService {

    private final ProjectRepository projectRepository;
    private final JenkinsAdapter jenkinsAdapter;
    private final RuntimeAdapter runtimeAdapter;
    private final HealthAdapter healthAdapter;
    private final IntegrationMapper integrationMapper;

    public IntegrationService(
            ProjectRepository projectRepository,
            JenkinsAdapter jenkinsAdapter,
            RuntimeAdapter runtimeAdapter,
            HealthAdapter healthAdapter,
            IntegrationMapper integrationMapper) {
        this.projectRepository = projectRepository;
        this.jenkinsAdapter = jenkinsAdapter;
        this.runtimeAdapter = runtimeAdapter;
        this.healthAdapter = healthAdapter;
        this.integrationMapper = integrationMapper;
    }

    public List<IntegrationStatusResponse> getStatuses(UUID projectId) {
        ProjectProfile project =
                projectRepository
                        .findById(projectId)
                        .orElseThrow(() -> new ProjectNotFoundException(projectId));

        IntegrationStatusResponse jenkins =
                integrationMapper.toResponse(jenkinsAdapter.inspect(project));
        IntegrationStatusResponse runtime =
                integrationMapper.toResponse(runtimeAdapter.inspect(project));
        IntegrationStatusResponse health =
                integrationMapper.toResponse(healthAdapter.inspect(project));

        return List.of(jenkins, runtime, health);
    }
}
