package com.yhcanbay.opsbridge.mapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.yhcanbay.opsbridge.dto.response.JenkinsBuildResponse;
import com.yhcanbay.opsbridge.dto.response.JenkinsChangeResponse;
import com.yhcanbay.opsbridge.dto.response.JenkinsStageResponse;
import com.yhcanbay.opsbridge.dto.response.JenkinsTestReportResponse;
import com.yhcanbay.opsbridge.enums.IntegrationState;
import com.yhcanbay.opsbridge.exception.JenkinsException;

import tools.jackson.databind.JsonNode;

@Component
public class JenkinsMapper {

    public JenkinsBuildResponse toBuildDto(JsonNode node, String jobUrl, String repositoryUrl) {
        if (!node.isObject() || node.path("number").asLong(0) < 1) {
            return null;
        }

        long number = node.path("number").asLong();
        boolean building = node.path("building").asBoolean(false);
        Instant startedAt = null;
        if (node.path("timestamp").isNumber() && node.path("timestamp").asLong() > 0) {
            startedAt = Instant.ofEpochMilli(node.path("timestamp").asLong());
        }

        Long duration = readDuration(node.path("duration"));
        if (building && startedAt != null) {
            duration = Math.max(0, Instant.now().toEpochMilli() - startedAt.toEpochMilli());
        }

        JenkinsBuildResponse response = JenkinsBuildResponse.builder()
                .number(number)
                .building(building)
                .result(node.path("result").asString("UNKNOWN"))
                .startedAt(startedAt)
                .durationMillis(duration)
                .url(jobUrl + number + "/")
                .consoleUrl(jobUrl + number + "/console")
                .changes(new ArrayList<>())
                .build();

        for (JsonNode action : node.path("actions")) {
            boolean matchingRepository = false;
            for (JsonNode remoteUrl : action.path("remoteUrls")) {
                if (normalizeRepositoryUrl(remoteUrl.asString("")).equals(normalizeRepositoryUrl(repositoryUrl))) {
                    matchingRepository = true;
                }
            }
            if (!matchingRepository) {
                continue;
            }
            JsonNode revision = action.path("lastBuiltRevision");
            String commitSha = revision.path("SHA1").asString("");
            if (!commitSha.isBlank()) {
                response.setCommitSha(commitSha);
                response.setCommitUrl(getCommitUrl(repositoryUrl, commitSha));
                for (JsonNode branch : revision.path("branch")) {
                    response.setBranch(branch.path("name").asString(""));
                    break;
                }
                break;
            }
        }

        List<JenkinsChangeResponse> changes = new ArrayList<>();
        addChanges(changes, node.path("changeSet"), repositoryUrl);
        for (JsonNode changeSet : node.path("changeSets")) {
            addChanges(changes, changeSet, repositoryUrl);
        }
        response.setChanges(changes);
        return response;
    }

    public List<JenkinsStageResponse> toStageDtos(JsonNode node) {
        if (!node.path("stages").isArray()) {
            throw invalidResponse("Jenkins aşama bilgisi geçerli bir liste içermiyor.");
        }

        List<JenkinsStageResponse> stages = new ArrayList<>();
        for (JsonNode stage : node.path("stages")) {
            String name = stage.path("name").asString("");
            if (name.isBlank()) {
                throw invalidResponse("Jenkins aşama adı boş döndü.");
            }
            JenkinsStageResponse response = JenkinsStageResponse.builder()
                    .name(name)
                    .status(stage.path("status").asString("UNKNOWN"))
                    .durationMillis(readDuration(stage.path("durationMillis")))
                    .build();
            stages.add(response);
        }
        return stages;
    }

    public JenkinsTestReportResponse toTestReportDto(JsonNode node, String buildUrl) {
        for (String field : List.of("passCount", "failCount", "skipCount")) {
            if (!node.path(field).isIntegralNumber() || node.path(field).asLong(-1) < 0
                    || node.path(field).asLong() > Integer.MAX_VALUE) {
                throw invalidResponse("Jenkins test raporundaki sayılar okunamadı.");
            }
        }

        int passed = node.path("passCount").asInt();
        int failed = node.path("failCount").asInt();
        int skipped = node.path("skipCount").asInt();
        long total = (long) passed + failed + skipped;
        if (total > Integer.MAX_VALUE) {
            throw invalidResponse("Jenkins test sayısı desteklenen aralığın dışında.");
        }

        List<String> failedTests = new ArrayList<>();
        for (JsonNode suite : node.path("suites")) {
            for (JsonNode test : suite.path("cases")) {
                String status = test.path("status").asString("");
                if (("FAILED".equals(status) || "REGRESSION".equals(status)) && failedTests.size() < 5) {
                    failedTests.add(test.path("className").asString("") + "."
                            + test.path("name").asString("Bilinmiyor"));
                }
            }
        }

        return JenkinsTestReportResponse.builder()
                .total((int) total)
                .passed(passed)
                .failed(failed)
                .skipped(skipped)
                .failedTests(failedTests)
                .url(buildUrl + "testReport/")
                .build();
    }

    private void addChanges(List<JenkinsChangeResponse> changes, JsonNode changeSet, String repositoryUrl) {
        for (JsonNode item : changeSet.path("items")) {
            if (changes.size() >= 20) {
                return;
            }
            String commitId = item.path("commitId").asString("");
            JenkinsChangeResponse change = JenkinsChangeResponse.builder()
                    .commitId(commitId)
                    .message(item.path("msg").asString("Mesaj bulunamadı"))
                    .author(item.path("author").path("fullName").asString("Bilinmiyor"))
                    .url(getCommitUrl(repositoryUrl, commitId))
                    .build();
            changes.add(change);
        }
    }

    private String getCommitUrl(String repositoryUrl, String commitSha) {
        if (repositoryUrl != null && repositoryUrl.matches("https://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/?")
                && commitSha.matches("[a-fA-F0-9]{7,64}")) {
            String repository = repositoryUrl.replaceAll("/+$", "").replaceAll("\\.git$", "");
            return repository + "/commit/" + commitSha;
        }
        return null;
    }

    private String normalizeRepositoryUrl(String repositoryUrl) {
        if (repositoryUrl == null) {
            return "";
        }
        return repositoryUrl.trim()
                .replaceFirst("^git@([^:]+):", "https://$1/")
                .replaceAll("/+$", "")
                .replaceAll("\\.git$", "");
    }

    private Long readDuration(JsonNode node) {
        if (node.isNumber() && node.asLong(-1) >= 0) {
            return node.asLong();
        }
        return null;
    }

    private JenkinsException invalidResponse(String message) {
        return new JenkinsException(IntegrationState.INVALID_RESPONSE, message);
    }
}
