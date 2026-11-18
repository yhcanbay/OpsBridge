package com.yhcanbay.opsbridge.mapper;

import org.springframework.stereotype.Component;

import com.yhcanbay.opsbridge.dto.response.GitHubConnectionResponse;
import com.yhcanbay.opsbridge.entity.GitHubConnection;

@Component
public class GitHubConnectionMapper {

    public GitHubConnectionResponse toDto(GitHubConnection connection) {
        return GitHubConnectionResponse.builder()
                .id(connection.getId())
                .name(connection.getName())
                .accountLogin(connection.getAccountLogin())
                .build();
    }
}
