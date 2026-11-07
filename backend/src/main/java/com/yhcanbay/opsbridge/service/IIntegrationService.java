package com.yhcanbay.opsbridge.service;

import java.util.List;
import java.util.UUID;

import com.yhcanbay.opsbridge.dto.response.IntegrationStatusResponse;

public interface IIntegrationService {

    List<IntegrationStatusResponse> getIntegrationStatuses(UUID projectId);
}
