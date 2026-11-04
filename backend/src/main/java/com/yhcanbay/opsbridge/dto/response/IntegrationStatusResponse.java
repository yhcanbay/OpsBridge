package com.yhcanbay.opsbridge.dto.response;

import com.yhcanbay.opsbridge.enums.IntegrationState;

import java.time.Instant;

public record IntegrationStatusResponse(
        String integration,
        IntegrationState state,
        String message,
        Instant lastSuccessfulFetchAt) {}
