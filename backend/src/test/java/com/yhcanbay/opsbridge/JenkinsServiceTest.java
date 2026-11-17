package com.yhcanbay.opsbridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import com.yhcanbay.opsbridge.config.JenkinsConfig;
import com.yhcanbay.opsbridge.dto.response.JenkinsJobResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.enums.Capability;
import com.yhcanbay.opsbridge.enums.IntegrationState;
import com.yhcanbay.opsbridge.exception.JenkinsException;
import com.yhcanbay.opsbridge.exception.ProjectNotFoundException;
import com.yhcanbay.opsbridge.mapper.JenkinsMapper;
import com.yhcanbay.opsbridge.repository.ProjectRepository;
import com.yhcanbay.opsbridge.service.impl.JenkinsServiceImpl;

class JenkinsServiceTest {

    private static final String REPOSITORY = "https://github.com/example/task-app";
    private static final String BUILD = """
            {"number":48,"building":false,"result":"FAILURE","timestamp":1794646800000,"duration":72000,
             "actions":[{"remoteUrls":["https://github.com/example/library"],"lastBuiltRevision":{"SHA1":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"}},
                        {"remoteUrls":["git@github.com:example/task-app.git"],"lastBuiltRevision":{"SHA1":"a8f3c210123456789012345678901234567890123","branch":[{"name":"origin/main"}]}}],
             "changeSets":[{"items":[{"commitId":"a8f3c210123456789012345678901234567890123","msg":"Fix tasks","author":{"fullName":"Developer"}}]}]}
            """;
    private static final String JOB = """
            {"name":"task-app","lastBuild":{"number":48,"building":true,"timestamp":1794646800000,"duration":0},
             "lastCompletedBuild":{"number":47,"result":"FAILURE","timestamp":1794646500000,"duration":72000},
             "lastSuccessfulBuild":{"number":46,"result":"SUCCESS","timestamp":1794646200000,"duration":128000},
             "builds":[{"number":48,"building":true},{"number":47,"result":"FAILURE","duration":72000},{"number":46,"result":"SUCCESS","duration":128000}]}
            """;

    private final UUID projectId = UUID.randomUUID();
    private ProjectRepository repository;
    private ProjectProfile project;
    private JenkinsConfig config;
    private MockRestServiceServer server;
    private JenkinsServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ProjectRepository.class);
        project = new ProjectProfile();
        project.setEnabled(true);
        project.setCapabilities(Set.of(Capability.BUILD_VIEW));
        project.setRepositoryUrl(REPOSITORY);
        project.setJenkinsBaseUrlRef("http://jenkins.test/ci");
        project.setJenkinsJobPath("apps/task-app");
        when(repository.findById(projectId)).thenReturn(Optional.of(project));
        config = new JenkinsConfig();
        config.setBaseUrl("http://jenkins.test/ci");
        config.setUsername("test-user");
        config.setApiToken("test-token");
        createService();
    }

    @Test
    void readsBuildStagesTestsAndOnlyTheMatchingRepositoryWithoutReturningCredentials() {
        expectJson("/ci/job/apps/job/task-app/api/json", JOB);
        expectJson("/ci/job/apps/job/task-app/48/api/json", BUILD);
        expectJson("/ci/job/apps/job/task-app/48/wfapi/describe", """
                {"stages":[{"name":"Checkout","status":"SUCCESS","durationMillis":4000},
                           {"name":"Backend tests","status":"FAILED","durationMillis":68000}]}
                """);
        expectJson("/ci/job/apps/job/task-app/48/testReport/api/json", """
                {"passCount":41,"failCount":3,"skipCount":0,
                 "suites":[{"cases":[{"className":"TaskServiceTest","name":"updateTask","status":"FAILED"}]}]}
                """);
        JenkinsJobResponse result = service.getJenkinsJobInfo(projectId, null);

        assertThat(result.getLastBuild().isBuilding()).isTrue();
        assertThat(result.getLastCompletedBuild().getNumber()).isEqualTo(47);
        assertThat(result.getBuilds()).hasSize(2);
        assertThat(result.getSelectedBuild().getCommitSha()).startsWith("a8f3c21");
        assertThat(result.getSelectedBuild().getBranch()).isEqualTo("origin/main");
        assertThat(result.getSelectedBuild().getCommitUrl()).startsWith(REPOSITORY + "/commit/");
        assertThat(result.getSelectedBuild().getChanges()).hasSize(1);
        assertThat(result.getStages().get(1).getStatus()).isEqualTo("FAILED");
        assertThat(result.getTestReport().getTotal()).isEqualTo(44);
        assertThat(result.getTestReport().getFailedTests()).containsExactly("TaskServiceTest.updateTask");
        assertThat(result.getFetchedAt()).isNotNull();
        assertThat(result.toString()).doesNotContain("test-token", "test-user", "bbbbbbbb");
        server.verify();
    }

    @Test
    void missingOptionalEndpointsDoNotEraseTheBuildOrInventZeroFailedTests() {
        expectJson("/ci/job/apps/job/task-app/api/json", JOB);
        expectJson("/ci/job/apps/job/task-app/48/api/json", BUILD);
        expectStatus("/ci/job/apps/job/task-app/48/wfapi/describe", 404);
        expectStatus("/ci/job/apps/job/task-app/48/testReport/api/json", 404);

        JenkinsJobResponse result = service.getJenkinsJobInfo(projectId, null);
        assertThat(result.getSelectedBuild().getNumber()).isEqualTo(48);
        assertThat(result.getTestReport()).isNull();
        assertThat(result.getTestReportMessage()).contains("sonuçları bilinmiyor");
        assertThat(result.getStages()).isEmpty();
        assertThat(result.getStagesMessage()).contains("Pipeline REST API");
        server.verify();
    }

    @Test
    void canReadAnExplicitHistoricalBuild() {
        expectJson("/ci/job/apps/job/task-app/api/json", JOB);
        expectJson("/ci/job/apps/job/task-app/47/api/json", BUILD.replace("\"number\":48", "\"number\":47"));
        expectStatus("/ci/job/apps/job/task-app/47/wfapi/describe", 404);
        expectStatus("/ci/job/apps/job/task-app/47/testReport/api/json", 404);
        assertThat(service.getJenkinsJobInfo(projectId, 47L).getSelectedBuild().getNumber()).isEqualTo(47);
        server.verify();
    }

    @Test
    void anEmptyJobDoesNotRequestStagesOrTests() {
        expectJson("/ci/job/apps/job/task-app/api/json", "{\"name\":\"task-app\",\"builds\":[]}");
        JenkinsJobResponse result = service.getJenkinsJobInfo(projectId, null);
        assertThat(result.getSelectedBuild()).isNull();
        assertThat(result.getBuilds()).isEmpty();
        server.verify();
    }

    @Test
    void connectionCheckUsesOneGetAndDoesNotConfuseAFailedBuildWithConnectionFailure() {
        expectJson("/ci/job/apps/job/task-app/api/json", "{\"name\":\"task-app\"}");
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isEqualTo(IntegrationState.CONNECTED);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404, 500, 302})
    void upstreamErrorsBecomeConnectionStatesInsteadOfOpsBridgeAuthenticationFailures(int status) {
        expectStatus("/ci/job/apps/job/task-app/api/json", status);
        IntegrationState expectedState = IntegrationState.UNREACHABLE;
        if (status == 401 || status == 403) expectedState = IntegrationState.ACCESS_DENIED;
        if (status == 404) expectedState = IntegrationState.JOB_NOT_FOUND;
        if (status == 302) expectedState = IntegrationState.INVALID_RESPONSE;
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isEqualTo(expectedState);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "[]", "{}", "not-json"})
    void emptyOrMalformedJobResponsesDoNotAppearConnected(String body) {
        expectJson("/ci/job/apps/job/task-app/api/json", body);
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isNotEqualTo(IntegrationState.CONNECTED);
        server.verify();
    }

    @Test
    void connectionTimeoutIsReportedWithoutLeakingTheUnderlyingError() {
        server.expect(request -> {}).andRespond(MockRestResponseCreators.withException(
                new SocketTimeoutException("private upstream detail")));
        var result = service.getJenkinsConnectionStatus(project);
        assertThat(result.getState()).isEqualTo(IntegrationState.UNREACHABLE);
        assertThat(result.getMessage()).doesNotContain("private upstream detail");
        server.verify();
    }

    @Test
    void encodesFolderNamesOnceAndAcceptsNativeJenkinsJobPaths() {
        project.setJenkinsJobPath("/job/My Folder/job/Görev/");
        expectJson("/ci/job/My%20Folder/job/G%C3%B6rev/api/json", "{\"name\":\"Görev\"}");
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isEqualTo(IntegrationState.CONNECTED);
        server.verify();
    }

    @Test
    void projectUrlAndJobPathChangesApplyOnTheNextRequest() {
        project.setJenkinsBaseUrlRef("http://second.test/jenkins");
        project.setJenkinsJobPath("correct-job");
        config.setAllowedBaseUrls(List.of("http://second.test/jenkins"));
        server.expect(request -> {
            assertThat(request.getURI().getHost()).isEqualTo("second.test");
            assertThat(request.getURI().getPath()).isEqualTo("/jenkins/job/correct-job/api/json");
        }).andRespond(withSuccess("{\"name\":\"correct-job\"}", MediaType.APPLICATION_JSON));
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isEqualTo(IntegrationState.CONNECTED);
        server.verify();
    }

    @Test
    void neverSendsSharedCredentialsToAnUnapprovedUrl() {
        project.setJenkinsBaseUrlRef("http://wrong.test");
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isEqualTo(IntegrationState.NOT_CONNECTED);
        server.verify();
    }

    @Test
    void anonymousAccessDoesNotSendAnAuthorizationHeader() {
        config.setUsername("");
        config.setApiToken("");
        server.expect(request -> assertThat(request.getHeaders().getFirst("Authorization")).isNull())
                .andRespond(withSuccess("{\"name\":\"task-app\"}", MediaType.APPLICATION_JSON));
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isEqualTo(IntegrationState.CONNECTED);
        server.verify();
    }

    @Test
    void incompleteOrMalformedCredentialsAreConfigurationErrorsWithoutAnOutboundRequest() {
        config.setApiToken("");
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isEqualTo(IntegrationState.NOT_CONNECTED);
        config.setApiToken("test-token");
        config.setUsername("invalid:username");
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isEqualTo(IntegrationState.NOT_CONNECTED);
        server.verify();
    }

    @Test
    void localJenkinsReferenceStillUsesTheServerConfiguration() {
        project.setJenkinsBaseUrlRef("local-jenkins");
        expectJson("/ci/job/apps/job/task-app/api/json", "{\"name\":\"task-app\"}");
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isEqualTo(IntegrationState.CONNECTED);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://jenkins.test", "http://user:secret@jenkins.test", "http://jenkins.test?x=1", "http://jenkins.test/#fragment", "http://jenkins.test/%2e%2e", "http://jenkins.test:99999", "http://jenkins.test:0"})
    void rejectsUnsafeBaseUrlsWithoutMakingARequest(String url) {
        project.setJenkinsBaseUrlRef(url);
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isEqualTo(IntegrationState.NOT_CONNECTED);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"../other", "job/", "job/app/not-job/name", "app?x=1", "app//other", "app/%2e%2e"})
    void rejectsInvalidJobPathsWithoutMakingARequest(String path) {
        project.setJenkinsJobPath(path);
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isEqualTo(IntegrationState.NOT_CONNECTED);
        server.verify();
    }

    @Test
    void disabledUnsupportedMissingAndInvalidBuildRequestsDoNotCallJenkins() {
        project.setEnabled(false);
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isEqualTo(IntegrationState.DISABLED);
        project.setEnabled(true);
        project.setCapabilities(Set.of());
        assertThat(service.getJenkinsConnectionStatus(project).getState()).isEqualTo(IntegrationState.NOT_SUPPORTED);
        assertThatThrownBy(() -> service.getJenkinsJobInfo(projectId, 0L)).isInstanceOf(JenkinsException.class);
        when(repository.findById(projectId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getJenkinsJobInfo(projectId, null)).isInstanceOf(ProjectNotFoundException.class);
        server.verify();
    }

    private void createService() {
        RestClient.Builder builder = config.jenkinsRestClient().mutate();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new JenkinsServiceImpl(repository, builder.build(), new JenkinsMapper(), config);
    }

    private void expectJson(String path, String body) {
        server.expect(request -> {
            assertThat(request.getMethod().name()).isEqualTo("GET");
            assertThat(request.getURI().getRawPath()).isEqualTo(path);
            assertThat(request.getHeaders().getFirst("Authorization")).startsWith("Basic ");
        }).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectStatus(String path, int status) {
        server.expect(request -> assertThat(request.getURI().getRawPath()).isEqualTo(path))
                .andRespond(withStatus(HttpStatus.valueOf(status)));
    }
}
