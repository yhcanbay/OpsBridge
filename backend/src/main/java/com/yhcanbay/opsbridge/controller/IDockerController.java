package com.yhcanbay.opsbridge.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.yhcanbay.opsbridge.dto.response.DockerContainerResponse;

public interface IDockerController {

    @GetMapping("/containers")
    List<DockerContainerResponse> getProjectContainers(@PathVariable(name = "id") UUID projectId);
}
