package com.yhcanbay.opsbridge.integration.adapter.stub;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.enums.Capability;
import com.yhcanbay.opsbridge.integration.adapter.JenkinsAdapter;
import com.yhcanbay.opsbridge.integration.model.IntegrationResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StubJenkinsAdapter implements JenkinsAdapter {

    private final StubIntegrationStatusFactory statusFactory;

    @Override
    public IntegrationResult inspect(ProjectProfile project) {
        Set<Capability> capabilities = Set.of(Capability.BUILD_VIEW, Capability.BUILD_TRIGGER);
        return statusFactory.create(project, "Jenkins", capabilities);
    }
}
