package com.yhcanbay.opsbridge.integration.adapter;

import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.integration.model.IntegrationResult;

public interface RuntimeAdapter {
    IntegrationResult inspect(ProjectProfile project);
}
