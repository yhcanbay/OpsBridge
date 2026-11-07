package com.yhcanbay.opsbridge.dto.response;

import java.time.Instant;

import com.yhcanbay.opsbridge.enums.IntegrationState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntegrationStatusResponse {

    private String integration;

    private IntegrationState state;

    private String message;

    private Instant lastSuccessfulFetchAt;
}
