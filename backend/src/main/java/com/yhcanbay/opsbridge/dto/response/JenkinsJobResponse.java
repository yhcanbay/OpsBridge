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
public class JenkinsJobResponse {

    private String jobName;
    private String jobUrl;
    private JenkinsBuildResponse lastBuild;
    private JenkinsBuildResponse lastCompletedBuild;
    private JenkinsBuildResponse lastSuccessfulBuild;
    private JenkinsBuildResponse selectedBuild;
    private List<JenkinsBuildResponse> builds;
    private List<JenkinsStageResponse> stages;
    private String stagesMessage;
    private JenkinsTestReportResponse testReport;
    private String testReportMessage;
    private Instant fetchedAt;
}
