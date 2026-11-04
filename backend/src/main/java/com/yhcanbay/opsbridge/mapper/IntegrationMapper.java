package com.yhcanbay.opsbridge.mapper;

import com.yhcanbay.opsbridge.dto.response.IntegrationStatusResponse;
import com.yhcanbay.opsbridge.integration.model.IntegrationResult;

import org.springframework.stereotype.Component;

@Component
public class IntegrationMapper {

    public IntegrationStatusResponse toResponse(IntegrationResult result) {
        return new IntegrationStatusResponse(
                result.integration(),
                result.state(),
                result.message(),
                result.lastSuccessfulFetchAt());
    }
}
