package com.yhcanbay.opsbridge.controller.impl;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yhcanbay.opsbridge.controller.IJenkinsController;
import com.yhcanbay.opsbridge.dto.response.JenkinsJobResponse;
import com.yhcanbay.opsbridge.service.IJenkinsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/projects/{id}/jenkins")
@RequiredArgsConstructor
public class JenkinsControllerImpl implements IJenkinsController {

    private final IJenkinsService jenkinsService;

    @Override
    public JenkinsJobResponse getJenkinsJobInfo(UUID projectId, Long buildNumber) {
        return jenkinsService.getJenkinsJobInfo(projectId, buildNumber);
    }
}
