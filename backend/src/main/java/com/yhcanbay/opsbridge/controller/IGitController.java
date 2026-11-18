package com.yhcanbay.opsbridge.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.yhcanbay.opsbridge.dto.response.GitHistoryResponse;

public interface IGitController {

    @GetMapping
    GitHistoryResponse getGitHistory(
            @PathVariable(name = "id") UUID projectId,
            @RequestParam(name = "branch", defaultValue = "") String branch,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "branchPage", defaultValue = "1") int branchPage);
}
