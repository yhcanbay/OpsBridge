package com.yhcanbay.opsbridge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JenkinsChangeResponse {

    private String commitId;
    private String message;
    private String author;
    private String url;
}
