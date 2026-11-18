package com.yhcanbay.opsbridge.mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HealthCheck;
import com.github.dockerjava.api.model.NetworkSettings;
import com.github.dockerjava.api.model.Ports.Binding;
import com.yhcanbay.opsbridge.dto.response.DockerContainerResponse;

@Component
public class DockerMapper {

    public DockerContainerResponse toDto(InspectContainerResponse container) {
        ContainerState state = container.getState();
        ContainerConfig config = container.getConfig();
        String name = container.getName();
        if (name != null && name.startsWith("/")) {
            name = name.substring(1);
        }

        String status = state.getStatus();
        Instant finishedAt = null;
        Long exitCode = null;
        boolean stopped = "exited".equals(status) || "dead".equals(status);
        if (stopped || "restarting".equals(status)) {
            finishedAt = parseDockerTime(state.getFinishedAt());
            exitCode = state.getExitCodeLong();
        }

        Map<String, String> labels = config.getLabels();
        String serviceName = labels == null ? null : labels.get("com.docker.compose.service");

        return DockerContainerResponse.builder()
                .id(container.getId())
                .name(name)
                .serviceName(serviceName)
                .state(status)
                .health(getHealthStatus(config, state))
                .image(config.getImage())
                .imageId(container.getImageId())
                .startedAt(parseDockerTime(state.getStartedAt()))
                .finishedAt(finishedAt)
                .exitCode(exitCode)
                .ports(getPublishedPorts(container.getNetworkSettings(), status))
                .build();
    }

    private String getHealthStatus(ContainerConfig config, ContainerState state) {
        HealthCheck check = config.getHealthcheck();
        if (check == null || check.getTest() == null || check.getTest().isEmpty()
                || "NONE".equals(check.getTest().getFirst())) {
            return "none";
        }

        if (!"running".equals(state.getStatus())) {
            return "inactive";
        }
        if (state.getHealth() == null || state.getHealth().getStatus() == null) {
            return "unknown";
        }
        return state.getHealth().getStatus();
    }

    private Instant parseDockerTime(String time) {
        // Docker henüz gerçekleşmeyen başlangıç veya bitiş için sıfır tarih döndürür.
        if (time == null || time.isBlank() || time.startsWith("0001-01-01")) {
            return null;
        }
        return Instant.parse(time);
    }

    private List<String> getPublishedPorts(NetworkSettings network, String status) {
        if (!"running".equals(status) && !"paused".equals(status)) {
            return List.of();
        }
        if (network == null || network.getPorts() == null) {
            return List.of();
        }

        Set<String> ports = new LinkedHashSet<>();
        for (Map.Entry<ExposedPort, Binding[]> port : network.getPorts().getBindings().entrySet()) {
            Binding[] bindings = port.getValue();
            if (bindings == null) {
                continue;
            }
            for (Binding binding : bindings) {
                String hostPort = binding.getHostPortSpec();
                if (hostPort != null && !hostPort.isBlank()) {
                    ports.add(hostPort + " → " + port.getKey());
                }
            }
        }
        return new ArrayList<>(ports);
    }
}
