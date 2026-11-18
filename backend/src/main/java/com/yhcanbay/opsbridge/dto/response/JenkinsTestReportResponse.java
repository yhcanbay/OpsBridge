package com.yhcanbay.opsbridge.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JenkinsTestReportResponse {

    private int total;
    private int passed;
    private int failed;
    private int skipped;
    private List<String> failedTests;
    private String url;
}
