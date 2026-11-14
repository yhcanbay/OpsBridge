package com.yhcanbay.opsbridge.dto.response;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.yhcanbay.opsbridge.enums.Capability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponse {

    private UUID id;

    private String name;

    private String repositoryUrl;

    private UUID githubConnectionId;

    private String environment;

    private String jenkinsBaseUrlRef;

    private String jenkinsJobPath;

    private String containerLabelSelector;

    private String healthUrl;

    private Set<Capability> capabilities;

    private boolean enabled;

    private Instant createdAt;

    private Instant updatedAt;
}
