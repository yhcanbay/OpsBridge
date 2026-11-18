package com.yhcanbay.opsbridge.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.yhcanbay.opsbridge.dto.request.GitHubConnectionRequest;
import com.yhcanbay.opsbridge.dto.response.GitHubConnectionResponse;

import jakarta.validation.Valid;

public interface IGitHubConnectionController {

    @GetMapping
    List<GitHubConnectionResponse> getAllConnections();

    @PostMapping
    ResponseEntity<GitHubConnectionResponse> saveConnection(@Valid @RequestBody GitHubConnectionRequest request);

    @PutMapping("/{id}")
    GitHubConnectionResponse updateConnection(@PathVariable(name = "id") UUID connectionId,
            @Valid @RequestBody GitHubConnectionRequest request);

    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteConnection(@PathVariable(name = "id") UUID connectionId);
}
