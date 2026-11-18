package com.yhcanbay.opsbridge.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JenkinsBuildResponse {

    private long number;
    private boolean building;
    private String result;
    private Instant startedAt;
    private Long durationMillis;
    private String url;
    private String consoleUrl;
    private String branch;
    private String commitSha;
    private String commitUrl;
    private List<JenkinsChangeResponse> changes;
}
