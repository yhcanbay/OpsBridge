package com.yhcanbay.opsbridge.service;

import java.util.List;
import java.util.UUID;

import com.yhcanbay.opsbridge.dto.request.GitHubConnectionRequest;
import com.yhcanbay.opsbridge.dto.response.GitHubConnectionResponse;

public interface IGitHubConnectionService {

    List<GitHubConnectionResponse> getAllConnections();

    GitHubConnectionResponse saveConnection(GitHubConnectionRequest request);

    GitHubConnectionResponse updateConnection(UUID connectionId, GitHubConnectionRequest request);

    void deleteConnection(UUID connectionId);

    // Yalnız backend servisleri kullanır; controller bu metodu dışarı açmaz.
    String getToken(UUID connectionId);
}
