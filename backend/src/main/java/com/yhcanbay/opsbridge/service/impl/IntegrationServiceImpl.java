package com.yhcanbay.opsbridge.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yhcanbay.opsbridge.dto.response.IntegrationStatusResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.enums.Capability;
import com.yhcanbay.opsbridge.enums.IntegrationState;
import com.yhcanbay.opsbridge.exception.ProjectNotFoundException;
import com.yhcanbay.opsbridge.repository.ProjectRepository;
import com.yhcanbay.opsbridge.service.IIntegrationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntegrationServiceImpl implements IIntegrationService {

    private final ProjectRepository projectRepository;

    @Override
    public List<IntegrationStatusResponse> getIntegrationStatuses(UUID projectId) {
        Optional<ProjectProfile> optionalProject = projectRepository.findById(projectId);

        if (optionalProject.isEmpty()) {
            throw new ProjectNotFoundException(projectId);
        }

        ProjectProfile project = optionalProject.get();
        Set<Capability> capabilities = project.getCapabilities();

        boolean jenkinsSupported = capabilities.contains(Capability.BUILD_VIEW)
                || capabilities.contains(Capability.BUILD_TRIGGER);
        boolean dockerSupported = capabilities.contains(Capability.CONTAINER_VIEW);
        boolean healthSupported = capabilities.contains(Capability.HEALTH_VIEW);

        IntegrationStatusResponse jenkinsStatus = createIntegrationStatus(project, "Jenkins", jenkinsSupported);
        IntegrationStatusResponse dockerStatus = createIntegrationStatus(project, "Docker", dockerSupported);
        IntegrationStatusResponse healthStatus = createIntegrationStatus(project, "HTTP Health", healthSupported);

        List<IntegrationStatusResponse> dtoList = new ArrayList<>();
        dtoList.add(jenkinsStatus);
        dtoList.add(dockerStatus);
        dtoList.add(healthStatus);

        return dtoList;
    }

    private IntegrationStatusResponse createIntegrationStatus(
            ProjectProfile project, String integrationName, boolean supported) {
        IntegrationStatusResponse response = IntegrationStatusResponse.builder()
                .integration(integrationName)
                .build();

        if (!project.isEnabled()) {
            response.setState(IntegrationState.DISABLED);
            response.setMessage("Proje pasif.");
            return response;
        }

        if (!supported) {
            response.setState(IntegrationState.NOT_SUPPORTED);
            response.setMessage("Profilde bu yetenek tanımlı değil.");
            return response;
        }

        response.setState(IntegrationState.NOT_CONNECTED);
        response.setMessage("Henüz bağlanmadı; bu sürüm dış sisteme istek göndermez.");
        return response;
    }
}
