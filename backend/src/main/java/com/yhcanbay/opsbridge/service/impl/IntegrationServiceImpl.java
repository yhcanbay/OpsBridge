package com.yhcanbay.opsbridge.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yhcanbay.opsbridge.dto.response.IntegrationStatusResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.exception.ProjectNotFoundException;
import com.yhcanbay.opsbridge.integration.adapter.HealthAdapter;
import com.yhcanbay.opsbridge.integration.adapter.JenkinsAdapter;
import com.yhcanbay.opsbridge.integration.adapter.RuntimeAdapter;
import com.yhcanbay.opsbridge.integration.model.IntegrationResult;
import com.yhcanbay.opsbridge.mapper.IntegrationMapper;
import com.yhcanbay.opsbridge.repository.ProjectRepository;
import com.yhcanbay.opsbridge.service.IIntegrationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntegrationServiceImpl implements IIntegrationService {

    private final ProjectRepository projectRepository;
    private final JenkinsAdapter jenkinsAdapter;
    private final RuntimeAdapter runtimeAdapter;
    private final HealthAdapter healthAdapter;
    private final IntegrationMapper integrationMapper;

    @Override
    public List<IntegrationStatusResponse> getIntegrationStatuses(UUID projectId) {
        Optional<ProjectProfile> optionalProject = projectRepository.findById(projectId);

        if (optionalProject.isEmpty()) {
            throw new ProjectNotFoundException(projectId);
        }

        ProjectProfile project = optionalProject.get();
        IntegrationResult jenkinsResult = jenkinsAdapter.inspect(project);
        IntegrationResult runtimeResult = runtimeAdapter.inspect(project);
        IntegrationResult healthResult = healthAdapter.inspect(project);

        List<IntegrationStatusResponse> dtoList = new ArrayList<>();
        dtoList.add(integrationMapper.toDto(jenkinsResult));
        dtoList.add(integrationMapper.toDto(runtimeResult));
        dtoList.add(integrationMapper.toDto(healthResult));

        return dtoList;
    }
}
