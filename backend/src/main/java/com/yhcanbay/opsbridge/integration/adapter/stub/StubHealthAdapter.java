package com.yhcanbay.opsbridge.integration.adapter.stub;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.enums.Capability;
import com.yhcanbay.opsbridge.integration.adapter.HealthAdapter;
import com.yhcanbay.opsbridge.integration.model.IntegrationResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StubHealthAdapter implements HealthAdapter {

    private final StubIntegrationStatusFactory statusFactory;

    @Override
    public IntegrationResult inspect(ProjectProfile project) {
        return statusFactory.create(project, "HTTP Health", Set.of(Capability.HEALTH_VIEW));
    }
}
