package com.yhcanbay.opsbridge.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.yhcanbay.opsbridge.dto.response.JenkinsJobResponse;

public interface IJenkinsController {

    @GetMapping
    JenkinsJobResponse getJenkinsJobInfo(
            @PathVariable(name = "id") UUID projectId,
            @RequestParam(name = "build", required = false) Long buildNumber);
}
