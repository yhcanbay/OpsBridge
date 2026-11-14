package com.yhcanbay.opsbridge.service;

import java.util.UUID;

import com.yhcanbay.opsbridge.dto.response.GitHistoryResponse;

public interface IGitService {

    GitHistoryResponse getGitHistory(UUID projectId, String branch, int page, int branchPage);
}
