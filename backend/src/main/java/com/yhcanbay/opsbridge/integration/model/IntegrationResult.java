package com.yhcanbay.opsbridge.integration.model;

import com.yhcanbay.opsbridge.enums.IntegrationState;

import java.time.Instant;

public record IntegrationResult(
        String integration,
        IntegrationState state,
        String message,
        Instant lastSuccessfulFetchAt) {}
