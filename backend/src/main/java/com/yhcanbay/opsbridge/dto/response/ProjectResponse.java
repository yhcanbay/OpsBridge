package com.yhcanbay.opsbridge.dto.response;

import com.yhcanbay.opsbridge.enums.Capability;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String repositoryUrl,
        String environment,
        String jenkinsBaseUrlRef,
        String jenkinsJobPath,
        String containerLabelSelector,
        String healthUrl,
        Set<Capability> capabilities,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {}
