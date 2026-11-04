package com.yhcanbay.opsbridge.integration.adapter.stub;

import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.enums.Capability;
import com.yhcanbay.opsbridge.integration.adapter.RuntimeAdapter;
import com.yhcanbay.opsbridge.integration.model.IntegrationResult;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class StubRuntimeAdapter implements RuntimeAdapter {

    private final StubIntegrationStatusFactory statusFactory;

    public StubRuntimeAdapter(StubIntegrationStatusFactory statusFactory) {
        this.statusFactory = statusFactory;
    }

    @Override
    public IntegrationResult inspect(ProjectProfile project) {
        return statusFactory.create(project, "Docker", Set.of(Capability.CONTAINER_VIEW));
    }
}
