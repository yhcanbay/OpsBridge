package com.yhcanbay.opsbridge.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DockerContainerResponse {

    private String id;
    private String name;
    private String serviceName;
    private String state;
    private String health;
    private String image;
    private String imageId;
    private Instant startedAt;
    private Instant finishedAt;
    private Long exitCode;
    private List<String> ports;
}
