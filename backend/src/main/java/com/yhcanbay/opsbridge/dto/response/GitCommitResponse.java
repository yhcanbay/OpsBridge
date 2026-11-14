package com.yhcanbay.opsbridge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GitCommitResponse {

    private String sha;

    private String message;

    private String author;

    private String committedAt;
}
