package com.yhcanbay.opsbridge.controller.impl;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yhcanbay.opsbridge.controller.IGitHubConnectionController;
import com.yhcanbay.opsbridge.dto.request.GitHubConnectionRequest;
import com.yhcanbay.opsbridge.dto.response.GitHubConnectionResponse;
import com.yhcanbay.opsbridge.service.IGitHubConnectionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/github-connections")
@RequiredArgsConstructor
public class GitHubConnectionControllerImpl implements IGitHubConnectionController {

    private final IGitHubConnectionService connectionService;

    @Override
    public List<GitHubConnectionResponse> getAllConnections() {
        return connectionService.getAllConnections();
    }

    @Override
    public ResponseEntity<GitHubConnectionResponse> saveConnection(GitHubConnectionRequest request) {
        GitHubConnectionResponse connection = connectionService.saveConnection(request);
        return ResponseEntity.created(URI.create("/api/github-connections/" + connection.getId())).body(connection);
    }

    @Override
    public GitHubConnectionResponse updateConnection(UUID connectionId, GitHubConnectionRequest request) {
        return connectionService.updateConnection(connectionId, request);
    }

    @Override
    public ResponseEntity<Void> deleteConnection(UUID connectionId) {
        connectionService.deleteConnection(connectionId);
        return ResponseEntity.noContent().build();
    }
}
