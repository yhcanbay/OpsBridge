package com.yhcanbay.opsbridge.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GitHubConnectionResponse {

    private UUID id;

    private String name;

    private String accountLogin;
}
