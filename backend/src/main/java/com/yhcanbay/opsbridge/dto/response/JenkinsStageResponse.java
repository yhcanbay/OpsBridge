package com.yhcanbay.opsbridge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JenkinsStageResponse {

    private String name;
    private String status;
    private Long durationMillis;
}
