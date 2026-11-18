package com.yhcanbay.opsbridge.controller.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yhcanbay.opsbridge.controller.IDockerController;
import com.yhcanbay.opsbridge.dto.response.DockerContainerResponse;
import com.yhcanbay.opsbridge.service.IDockerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/projects/{id}/docker")
@RequiredArgsConstructor
public class DockerControllerImpl implements IDockerController {

    private final IDockerService dockerService;

    @Override
    public List<DockerContainerResponse> getProjectContainers(UUID projectId) {
        return dockerService.getProjectContainers(projectId);
    }
}
