package com.yhcanbay.opsbridge.service.impl;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.yhcanbay.opsbridge.config.JenkinsConfig;
import com.yhcanbay.opsbridge.dto.response.IntegrationStatusResponse;
import com.yhcanbay.opsbridge.dto.response.JenkinsBuildResponse;
import com.yhcanbay.opsbridge.dto.response.JenkinsJobResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.enums.Capability;
import com.yhcanbay.opsbridge.enums.IntegrationState;
import com.yhcanbay.opsbridge.exception.JenkinsException;
import com.yhcanbay.opsbridge.exception.ProjectNotFoundException;
import com.yhcanbay.opsbridge.mapper.JenkinsMapper;
import com.yhcanbay.opsbridge.repository.ProjectRepository;
import com.yhcanbay.opsbridge.service.IJenkinsService;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class JenkinsServiceImpl implements IJenkinsService {

    private static final String BUILD_FIELDS = "number,building,result,timestamp,duration,"
            + "actions[remoteUrls,lastBuiltRevision[SHA1,branch[name]]]";
    private static final String JOB_FIELDS = "name,lastBuild[" + BUILD_FIELDS + "],"
            + "lastCompletedBuild[" + BUILD_FIELDS + "],lastSuccessfulBuild[" + BUILD_FIELDS + "],"
            + "builds[" + BUILD_FIELDS + "]{0,30}";
    private static final String CHANGE_FIELDS = "items[commitId,msg,author[fullName]]{0,20}";

    private final ProjectRepository projectRepository;
    private final RestClient jenkinsRestClient;
    private final JenkinsMapper jenkinsMapper;
    private final JenkinsConfig jenkinsConfig;

    @Override
    public IntegrationStatusResponse getJenkinsConnectionStatus(ProjectProfile project) {
        IntegrationStatusResponse response = IntegrationStatusResponse.builder()
                .integration("Jenkins")
                .build();
        try {
            String jobUrl = getJobUrl(project);
            JsonNode job = getJson(jobUrl, "api/json", "name");
            validateJob(job);
            response.setState(IntegrationState.CONNECTED);
            response.setMessage("Jenkins job bağlantısı doğrulandı. Ayrıntıları açabilirsiniz.");
            response.setLastSuccessfulFetchAt(Instant.now());
        } catch (JenkinsException exception) {
            response.setState(exception.getState());
            response.setMessage(exception.getMessage());
        }
        return response;
    }

    @Override
    public JenkinsJobResponse getJenkinsJobInfo(UUID projectId, Long buildNumber) {
        if (buildNumber != null && buildNumber < 1) {
            throw new JenkinsException(IntegrationState.NOT_CONNECTED, "Build numarası en az 1 olmalıdır.");
        }
        Optional<ProjectProfile> optionalProject = projectRepository.findById(projectId);
        if (optionalProject.isEmpty()) {
            throw new ProjectNotFoundException(projectId);
        }

        ProjectProfile project = optionalProject.get();
        String jobUrl = getJobUrl(project);
        String repositoryUrl = project.getRepositoryUrl();
        JsonNode job = getJson(jobUrl, "api/json", JOB_FIELDS);
        validateJob(job);

        JenkinsBuildResponse lastBuild = jenkinsMapper.toBuildDto(job.path("lastBuild"), jobUrl, repositoryUrl);
        JenkinsJobResponse response = JenkinsJobResponse.builder()
                .jobName(job.path("name").asString())
                .jobUrl(jobUrl)
                .lastBuild(lastBuild)
                .lastCompletedBuild(jenkinsMapper.toBuildDto(job.path("lastCompletedBuild"), jobUrl, repositoryUrl))
                .lastSuccessfulBuild(jenkinsMapper.toBuildDto(job.path("lastSuccessfulBuild"), jobUrl, repositoryUrl))
                .builds(new ArrayList<>())
                .stages(new ArrayList<>())
                .build();

        List<JenkinsBuildResponse> builds = new ArrayList<>();
        for (JsonNode buildNode : job.path("builds")) {
            JenkinsBuildResponse build = jenkinsMapper.toBuildDto(buildNode, jobUrl, repositoryUrl);
            if (build != null && !build.isBuilding() && builds.size() < 10) {
                builds.add(build);
            }
        }
        response.setBuilds(builds);

        Long selectedNumber = buildNumber;
        if (selectedNumber == null && lastBuild != null) {
            selectedNumber = lastBuild.getNumber();
        }
        if (selectedNumber != null) {
            String selectedPath = selectedNumber + "/";
            JsonNode buildNode = getJson(jobUrl, selectedPath + "api/json",
                    BUILD_FIELDS + ",changeSet[" + CHANGE_FIELDS + "],changeSets[" + CHANGE_FIELDS + "]");
            JenkinsBuildResponse selectedBuild = jenkinsMapper.toBuildDto(buildNode, jobUrl, repositoryUrl);
            if (selectedBuild == null || selectedBuild.getNumber() != selectedNumber) {
                throw new JenkinsException(IntegrationState.INVALID_RESPONSE, "Jenkins build bilgisi okunamadı.");
            }
            response.setSelectedBuild(selectedBuild);
            readStages(response, jobUrl, selectedPath);
            readTestReport(response, jobUrl, selectedPath);
        }

        response.setFetchedAt(Instant.now());
        return response;
    }

    private void readStages(JenkinsJobResponse response, String jobUrl, String buildPath) {
        try {
            JsonNode stages = getJson(jobUrl, buildPath + "wfapi/describe", null);
            response.setStages(jenkinsMapper.toStageDtos(stages));
        } catch (JenkinsException exception) {
            if (exception.getState() == IntegrationState.JOB_NOT_FOUND) {
                response.setStagesMessage("Aşama bilgisi bulunamadı. Pipeline REST API eklentisi veya bu build'in aşama kaydı mevcut olmayabilir.");
            } else {
                response.setStagesMessage("Aşama bilgisi alınamadı. " + exception.getMessage());
            }
        }
    }

    private void readTestReport(JenkinsJobResponse response, String jobUrl, String buildPath) {
        try {
            JsonNode report = getJson(jobUrl, buildPath + "testReport/api/json",
                    "passCount,failCount,skipCount,suites[cases[className,name,status]]");
            response.setTestReport(jenkinsMapper.toTestReportDto(report, jobUrl + buildPath));
        } catch (JenkinsException exception) {
            if (exception.getState() == IntegrationState.JOB_NOT_FOUND) {
                response.setTestReportMessage("Test raporu bulunamadı. Bu build için yayımlanmış bir rapor yok; test sonuçları bilinmiyor.");
            } else {
                response.setTestReportMessage("Test raporu alınamadı. " + exception.getMessage());
            }
        }
    }

    private JsonNode getJson(String jobUrl, String path, String tree) {
        try {
            String requestUrl = jobUrl + path;
            if (tree != null) {
                requestUrl += "?tree=" + UriUtils.encodeQueryParam(tree, StandardCharsets.UTF_8);
            }
            URI uri = URI.create(requestUrl);
            JsonNode response = jenkinsRestClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.isObject()) {
                throw new JenkinsException(IntegrationState.INVALID_RESPONSE, "Jenkins boş veya geçersiz yanıt döndürdü.");
            }
            return response;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new JenkinsException(IntegrationState.ACCESS_DENIED,
                        "Jenkins erişimi reddedildi. Sunucudaki Jenkins kullanıcı adı, API tokenı ve job okuma iznini kontrol edin.");
            }
            if (status == 404) {
                throw new JenkinsException(IntegrationState.JOB_NOT_FOUND,
                        "Jenkins job veya build bulunamadı. Proje ayarlarındaki URL ve job yolunu kontrol edin.");
            }
            throw new JenkinsException(IntegrationState.UNREACHABLE, "Jenkins isteği tamamlanamadı. Sunucu yanıt kodu: " + status + ".");
        } catch (RestClientException exception) {
            if (exception.getCause() instanceof HttpMessageConversionException) {
                throw new JenkinsException(IntegrationState.INVALID_RESPONSE, "Jenkins geçerli JSON yanıtı döndürmedi.");
            }
            throw new JenkinsException(IntegrationState.UNREACHABLE,
                    "Jenkins'e ulaşılamadı veya yanıt okunamadı. URL, bağlantı ve sunucu durumunu kontrol edin.");
        }
    }

    private String getJobUrl(ProjectProfile project) {
        if (!project.isEnabled()) {
            throw new JenkinsException(IntegrationState.DISABLED, "Proje pasif. Jenkins bağlantısı kontrol edilmedi.");
        }
        if (!project.getCapabilities().contains(Capability.BUILD_VIEW)
                && !project.getCapabilities().contains(Capability.BUILD_TRIGGER)) {
            throw new JenkinsException(IntegrationState.NOT_SUPPORTED, "Profilde Jenkins görüntüleme yeteneği tanımlı değil.");
        }

        String baseUrl = project.getJenkinsBaseUrlRef();
        String jobPath = project.getJenkinsJobPath();
        if (baseUrl == null || baseUrl.isBlank() || jobPath == null || jobPath.isBlank()) {
            throw new JenkinsException(IntegrationState.NOT_CONNECTED, "Proje ayarlarından Jenkins URL ve job yolunu tanımlayın.");
        }
        if ("local-jenkins".equals(baseUrl)) {
            baseUrl = jenkinsConfig.getBaseUrl();
        }
        baseUrl = baseUrl.trim();
        try {
            URI uri = URI.create(baseUrl.trim());
            if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getPort() == 0 || uri.getPort() > 65535
                    || uri.getFragment() != null || uri.getPath().contains("..") || uri.getRawPath().contains("%")) {
                throw new IllegalArgumentException();
            }
            String cleanPath = jobPath.trim().replaceAll("^/+|/+$", "");
            String[] segments = cleanPath.split("/", -1);
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl.replaceAll("/+$", ""));
            boolean jenkinsPath = jobPath.trim().replaceAll("^/+", "").startsWith("job/");
            for (int index = 0; index < segments.length; index++) {
                String segment = segments[index];
                if (jenkinsPath && index % 2 == 0) {
                    if (!"job".equals(segment) || index == segments.length - 1) {
                        throw new IllegalArgumentException();
                    }
                    continue;
                }
                if (segment.isBlank() || segment.equals(".") || segment.equals("..")
                        || segment.matches(".*[\\\\?#%:].*")) {
                    throw new IllegalArgumentException();
                }
                builder.pathSegment("job", segment);
            }
            return builder.build().encode().toUriString() + "/";
        } catch (IllegalArgumentException exception) {
            throw new JenkinsException(IntegrationState.NOT_CONNECTED,
                    "Jenkins URL veya job yolu geçersiz. HTTP(S) URL ve folder/job-name biçiminde bir job yolu girin.");
        }
    }

    private void validateJob(JsonNode job) {
        if (job.path("name").asString("").isBlank()) {
            throw new JenkinsException(IntegrationState.INVALID_RESPONSE, "Jenkins yanıtında job adı bulunamadı.");
        }
    }
}
