package com.yhcanbay.opsbridge.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.yhcanbay.opsbridge.dto.response.DockerContainerResponse;
import com.yhcanbay.opsbridge.dto.response.IntegrationStatusResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.enums.Capability;
import com.yhcanbay.opsbridge.enums.IntegrationState;
import com.yhcanbay.opsbridge.exception.DockerIntegrationException;
import com.yhcanbay.opsbridge.exception.ProjectNotFoundException;
import com.yhcanbay.opsbridge.mapper.DockerMapper;
import com.yhcanbay.opsbridge.repository.ProjectRepository;
import com.yhcanbay.opsbridge.service.IDockerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DockerServiceImpl implements IDockerService {

    private static final String COMPOSE_SELECTOR = "com\\.docker\\.compose\\.project=[a-z0-9][a-z0-9_-]*";
    private static final String CONNECTION_ERROR =
            "Docker bilgileri alınamadı. Docker Engine'i ve yerel bağlantı ayarını kontrol edin.";

    private final DockerClient dockerClient;
    private final ProjectRepository projectRepository;
    private final DockerMapper dockerMapper;

    @Override
    public List<DockerContainerResponse> getProjectContainers(UUID projectId) {
        ProjectProfile project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        String selector = getComposeSelector(project);
        List<DockerContainerResponse> responses = new ArrayList<>();

        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .withLabelFilter(List.of(selector))
                    .exec();

            for (Container container : containers) {
                InspectContainerResponse details;
                try {
                    details = dockerClient.inspectContainerCmd(container.getId()).exec();
                } catch (NotFoundException exception) {
                    // Container, listeleme ile ayrıntı isteği arasında kaldırılmış olabilir.
                    continue;
                }
                responses.add(dockerMapper.toDto(details));
            }
        } catch (RuntimeException exception) {
            throw new DockerIntegrationException(IntegrationState.UNREACHABLE, CONNECTION_ERROR);
        }

        return responses;
    }

    @Override
    public IntegrationStatusResponse getDockerConnectionStatus(ProjectProfile project) {
        IntegrationStatusResponse response = IntegrationStatusResponse.builder()
                .integration("Docker")
                .build();

        try {
            getComposeSelector(project);
            //pingCmd() bir komut nesnesi hazırlar. Docker Engine’e istek, .exec() çağrıldığında gider.
            dockerClient.pingCmd().exec();
            response.setState(IntegrationState.CONNECTED);
            response.setMessage("Docker Engine bağlantısı hazır.");
            response.setLastSuccessfulFetchAt(Instant.now());
        } catch (DockerIntegrationException exception) {
            response.setState(exception.getState());
            response.setMessage(exception.getMessage());
        } catch (RuntimeException exception) {
            response.setState(IntegrationState.UNREACHABLE);
            response.setMessage(CONNECTION_ERROR);
        }
        return response;
    }

    private String getComposeSelector(ProjectProfile project) {
        if (!project.isEnabled()) {
            throw new DockerIntegrationException(IntegrationState.DISABLED, "Proje pasif.");
        }
        if (!project.getCapabilities().contains(Capability.CONTAINER_VIEW)) {
            throw new DockerIntegrationException(
                    IntegrationState.NOT_SUPPORTED, "Profilde Container görüntüleme yeteneği tanımlı değil.");
        }

        String selector = project.getContainerLabelSelector();
        if (selector == null || !selector.trim().matches(COMPOSE_SELECTOR)) {
            throw new DockerIntegrationException(
                    IntegrationState.NOT_CONNECTED,
                    "Proje profilinde com.docker.compose.project=proje-adi biçiminde bir container etiketi girin.");
        }
        return selector.trim();
    }
}
