package com.yhcanbay.opsbridge.controller.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yhcanbay.opsbridge.controller.IIntegrationController;
import com.yhcanbay.opsbridge.dto.response.IntegrationStatusResponse;
import com.yhcanbay.opsbridge.service.IIntegrationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/projects/{id}/integrations")
@RequiredArgsConstructor
public class IntegrationControllerImpl implements IIntegrationController {

    private final IIntegrationService integrationService;

    @Override
    public List<IntegrationStatusResponse> getIntegrationStatuses(UUID projectId) {
        return integrationService.getIntegrationStatuses(projectId);
    }
}
