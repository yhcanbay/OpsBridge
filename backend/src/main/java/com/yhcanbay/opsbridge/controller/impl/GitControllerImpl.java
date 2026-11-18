package com.yhcanbay.opsbridge.controller.impl;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yhcanbay.opsbridge.controller.IGitController;
import com.yhcanbay.opsbridge.dto.response.GitHistoryResponse;
import com.yhcanbay.opsbridge.service.IGitService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/api/projects/{id}/git")
@RequiredArgsConstructor
public class GitControllerImpl implements IGitController {

    private final IGitService gitService;

    @Override
    public GitHistoryResponse getGitHistory(UUID projectId, String branch, int page, int branchPage) {
        return gitService.getGitHistory(projectId, branch, page, branchPage);
    }
}
