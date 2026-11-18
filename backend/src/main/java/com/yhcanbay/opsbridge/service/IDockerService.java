package com.yhcanbay.opsbridge.service;

import java.util.List;
import java.util.UUID;

import com.yhcanbay.opsbridge.dto.response.DockerContainerResponse;
import com.yhcanbay.opsbridge.dto.response.IntegrationStatusResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;

public interface IDockerService {

    List<DockerContainerResponse> getProjectContainers(UUID projectId);

    IntegrationStatusResponse getDockerConnectionStatus(ProjectProfile project);
}
