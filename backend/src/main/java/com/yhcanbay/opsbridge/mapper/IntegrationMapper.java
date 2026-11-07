package com.yhcanbay.opsbridge.mapper;

import org.springframework.stereotype.Component;

import com.yhcanbay.opsbridge.dto.response.IntegrationStatusResponse;
import com.yhcanbay.opsbridge.integration.model.IntegrationResult;

@Component
public class IntegrationMapper {

    public IntegrationStatusResponse toDto(IntegrationResult result) {
        return IntegrationStatusResponse.builder()
                .integration(result.getIntegration())
                .state(result.getState())
                .message(result.getMessage())
                .lastSuccessfulFetchAt(result.getLastSuccessfulFetchAt())
                .build();
    }
}
