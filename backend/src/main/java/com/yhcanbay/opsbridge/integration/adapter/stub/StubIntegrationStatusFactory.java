package com.yhcanbay.opsbridge.integration.adapter.stub;

import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.enums.Capability;
import com.yhcanbay.opsbridge.enums.IntegrationState;
import com.yhcanbay.opsbridge.integration.model.IntegrationResult;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class StubIntegrationStatusFactory {

    public IntegrationResult create(
            ProjectProfile project, String name, Set<Capability> supportedCapabilities) {
        if (!project.isEnabled()) {
            return new IntegrationResult(name, IntegrationState.DISABLED, "Proje pasif.", null);
        }

        boolean supported =
                supportedCapabilities.stream().anyMatch(project.getCapabilities()::contains);

        if (!supported) {
            return new IntegrationResult(
                    name,
                    IntegrationState.NOT_SUPPORTED,
                    "Profilde bu yetenek tanımlı değil.",
                    null);
        }

        return new IntegrationResult(
                name,
                IntegrationState.NOT_CONNECTED,
                "Henüz bağlanmadı; bu sürüm dış sisteme istek göndermez.",
                null);
    }
}
