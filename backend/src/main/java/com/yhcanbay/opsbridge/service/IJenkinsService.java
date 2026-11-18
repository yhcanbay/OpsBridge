package com.yhcanbay.opsbridge.service;

import java.util.UUID;

import com.yhcanbay.opsbridge.dto.response.IntegrationStatusResponse;
import com.yhcanbay.opsbridge.dto.response.JenkinsJobResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;

public interface IJenkinsService {

    IntegrationStatusResponse getJenkinsConnectionStatus(ProjectProfile project);

    JenkinsJobResponse getJenkinsJobInfo(UUID projectId, Long buildNumber);
}
