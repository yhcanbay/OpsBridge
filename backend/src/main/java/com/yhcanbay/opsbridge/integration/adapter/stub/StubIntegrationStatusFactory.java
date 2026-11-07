package com.yhcanbay.opsbridge.integration.adapter.stub;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.enums.Capability;
import com.yhcanbay.opsbridge.enums.IntegrationState;
import com.yhcanbay.opsbridge.integration.model.IntegrationResult;

@Component
public class StubIntegrationStatusFactory {

    public IntegrationResult create(
            ProjectProfile project, String name, Set<Capability> supportedCapabilities) {
        if (!project.isEnabled()) {
            return IntegrationResult.builder()
                    .integration(name)
                    .state(IntegrationState.DISABLED)
                    .message("Proje pasif.")
                    .build();
        }

        boolean supported = false;
        Set<Capability> projectCapabilities = project.getCapabilities();

        for (Capability capability : supportedCapabilities) {
            if (projectCapabilities.contains(capability)) {
                supported = true;
                break;
            }
        }

        if (!supported) {
            return IntegrationResult.builder()
                    .integration(name)
                    .state(IntegrationState.NOT_SUPPORTED)
                    .message("Profilde bu yetenek tanımlı değil.")
                    .build();
        }

        return IntegrationResult.builder()
                .integration(name)
                .state(IntegrationState.NOT_CONNECTED)
                .message("Henüz bağlanmadı; bu sürüm dış sisteme istek göndermez.")
                .build();
    }
}
