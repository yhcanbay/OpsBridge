package com.yhcanbay.opsbridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientConfig;
import com.yhcanbay.opsbridge.jwt.JwtService;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpsBridgeApiTest {
    private static final String REFERENCE_ID = "b946c162-581a-46ec-a961-066d69c974f1";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwtService;

    @MockitoBean HttpClient gitHubHttpClient;
    @MockitoBean DockerClient dockerClient;

    @Test
    void anonymousCannotReadProjectsButCanReadApplicationHealth() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/projects/" + REFERENCE_ID + "/git")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/projects/" + REFERENCE_ID + "/docker/containers"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void invalidPasswordIsRejected() throws Exception {
        mvc.perform(
                        post("/api/auth/login")
                                .with(validCsrf())
                                .param("username", "admin")
                                .param("password", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("OPSBRIDGE_AUTH"));
    }

    @Test
    void loginAndLogoutUseRealJwtAndCsrfCookies() throws Exception {
        mvc.perform(post("/api/auth/login").param("username", "admin")
                        .param("password", "integration-test-only"))
                .andExpect(status().isForbidden());

        Cookie authCookie = login();
        Cookie otherLogin = login();
        MvcResult csrfResult = csrfToken();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("OPSBRIDGE_CSRF");
        mvc.perform(get("/api/auth/me").cookie(authCookie, csrfCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(cookie().doesNotExist("OPSBRIDGE_CSRF"))
                .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());

        mvc.perform(post("/api/auth/logout").cookie(authCookie)).andExpect(status().isForbidden());
        JsonNode token = json.readTree(csrfResult.getResponse().getContentAsString());
        mvc.perform(
                        post("/api/auth/logout")
                                .cookie(authCookie, csrfCookie)
                                .header(
                                        token.get("headerName").asText(),
                                        token.get("token").asText()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("OPSBRIDGE_AUTH", 0))
                .andExpect(cookie().maxAge("OPSBRIDGE_CSRF", 0));
        mvc.perform(get("/api/auth/me").cookie(authCookie)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(otherLogin)).andExpect(status().isOk());
    }

    @Test
    void malformedForeignAndExpiredTokensAreRejected() throws Exception {
        SecretKey signingKey = (SecretKey) ReflectionTestUtils.getField(jwtService, "signingKey");
        Instant expiredAt = Instant.now().minusSeconds(60);
        String expiredToken = Jwts.builder().issuer("opsbridge").audience().add("opsbridge-api").and()
                .id("expired-for-test").subject("admin").expiration(Date.from(expiredAt))
                .signWith(signingKey, Jwts.SIG.HS256).compact();

        for (String token : List.of("invalid-token", new JwtService().generateToken("admin"), expiredToken)) {
            mvc.perform(get("/api/auth/me").cookie(new Cookie("OPSBRIDGE_AUTH", token)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void arbitraryProjectCanBeCreatedUpdatedReadAndDeletedAfterLogin() throws Exception {
        Cookie authCookie = login();
        String body = project("Another_Application", "[\"BUILD_VIEW\"]", true);
        mvc.perform(
                        post("/api/projects")
                                .cookie(authCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isForbidden());

        MvcResult csrfResult = csrfToken();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("OPSBRIDGE_CSRF");
        JsonNode token = json.readTree(csrfResult.getResponse().getContentAsString());
        String header = token.get("headerName").asText();
        String value = token.get("token").asText();
        MvcResult created =
                mvc.perform(
                                post("/api/projects")
                                        .cookie(authCookie, csrfCookie)
                                        .header(header, value)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andExpect(jsonPath("$.name").value("Another_Application"))
                        .andExpect(jsonPath("$.createdAt").exists())
                        .andReturn();
        String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
        String path = "/api/projects/" + id;
        mvc.perform(get(path).cookie(authCookie)).andExpect(status().isOk());
        mvc.perform(
                        put(path)
                                .cookie(authCookie, csrfCookie)
                                .header(header, value)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(project("Renamed_Application", "[]", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed_Application"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.capabilities").isEmpty());
        mvc.perform(delete(path).cookie(authCookie, csrfCookie).header(header, value))
                .andExpect(status().isNoContent());
        mvc.perform(get(path).cookie(authCookie)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void migrationSeedsReferenceProfileAndReportsUnavailableDocker() throws Exception {
        when(dockerClient.pingCmd()).thenThrow(new IllegalStateException("Docker bağlantısı kapalı."));
        mvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Personal_Task_App')]").isNotEmpty());
        mvc.perform(get("/api/projects/" + REFERENCE_ID + "/integrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].integration").value("Jenkins"))
                .andExpect(jsonPath("$[1].integration").value("Docker"))
                .andExpect(jsonPath("$[2].integration").value("HTTP Health"))
                .andExpect(jsonPath("$[0].state").value("NOT_CONNECTED"))
                .andExpect(jsonPath("$[1].state").value("UNREACHABLE"))
                .andExpect(jsonPath("$[2].state").value("NOT_CONNECTED"))
                .andExpect(jsonPath("$[0].lastSuccessfulFetchAt").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void readsOnlyTheConfiguredComposeProjectAndMapsContainerDetails() throws Exception {
        PingCmd pingCommand = mock(PingCmd.class);
        when(dockerClient.pingCmd()).thenReturn(pingCommand);
        ListContainersCmd listCommand = mock(ListContainersCmd.class, RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(listCommand);
        Container db = mock(Container.class);
        when(db.getId()).thenReturn("db-id");
        Container backend = mock(Container.class);
        when(backend.getId()).thenReturn("backend-id");
        when(listCommand.exec()).thenReturn(List.of(db, backend));
        mockDockerInspect("db-id", """
                {
                    "Id": "db-id", "Name": "/taskapp-db", "Image": "sha256:db-image",
                    "Config": {
                        "Image": "postgres:16-alpine",
                        "Labels": {"com.docker.compose.service": "db"},
                        "Healthcheck": {"Test": ["CMD", "pg_isready"]},
                        "Env": ["DB_PASSWORD=example-secret"]
                    },
                    "State": {
                        "Status": "running", "StartedAt": "2026-11-18T11:18:00.123Z",
                        "FinishedAt": "0001-01-01T00:00:00Z", "ExitCode": 0,
                        "Health": {"Status": "healthy"}
                    },
                    "NetworkSettings": {"Ports": {"5432/tcp": [
                        {"HostIp": "0.0.0.0", "HostPort": "5432"},
                        {"HostIp": "::", "HostPort": "5432"}
                    ], "9090/tcp": null}}
                }
                """);
        mockDockerInspect("backend-id", """
                {
                    "Id": "backend-id", "Name": "/taskapp-backend", "Image": "sha256:backend-image",
                    "Config": {"Image": "taskapp-backend:latest",
                               "Labels": {"com.docker.compose.service": "backend"}},
                    "State": {"Status": "exited", "StartedAt": "2026-11-18T11:18:00Z",
                              "FinishedAt": "2026-11-18T11:24:00Z", "ExitCode": 0},
                    "NetworkSettings": {"Ports": {"8080/tcp": [{"HostIp": "", "HostPort": "8080"}]}}
                }
                """);

        mvc.perform(get("/api/projects/" + REFERENCE_ID + "/integrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].state").value("CONNECTED"));
        mvc.perform(get("/api/projects/" + REFERENCE_ID + "/docker/containers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("taskapp-db"))
                .andExpect(jsonPath("$[0].serviceName").value("db"))
                .andExpect(jsonPath("$[0].state").value("running"))
                .andExpect(jsonPath("$[0].health").value("healthy"))
                .andExpect(jsonPath("$[0].image").value("postgres:16-alpine"))
                .andExpect(jsonPath("$[0].imageId").value("sha256:db-image"))
                .andExpect(jsonPath("$[0].startedAt").value("2026-11-18T11:18:00.123Z"))
                .andExpect(jsonPath("$[0].finishedAt").isEmpty())
                .andExpect(jsonPath("$[0].exitCode").isEmpty())
                .andExpect(jsonPath("$[0].ports.length()").value(1))
                .andExpect(jsonPath("$[0].ports[0]").value("5432 → 5432/tcp"))
                .andExpect(jsonPath("$[0].config").doesNotExist())
                .andExpect(jsonPath("$[1].state").value("exited"))
                .andExpect(jsonPath("$[1].health").value("none"))
                .andExpect(jsonPath("$[1].finishedAt").value("2026-11-18T11:24:00Z"))
                .andExpect(jsonPath("$[1].exitCode").value(0))
                .andExpect(jsonPath("$[1].ports").isEmpty());
        verify(listCommand).withShowAll(true);
        verify(listCommand).withLabelFilter(List.of("com.docker.compose.project=personal_task_app"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dockerReadDistinguishesAnEmptyProjectFromAnEngineError() throws Exception {
        ListContainersCmd listCommand = mock(ListContainersCmd.class, RETURNS_SELF);
        when(dockerClient.listContainersCmd()).thenReturn(listCommand);
        when(listCommand.exec()).thenReturn(List.of())
                .thenThrow(new IllegalStateException("Internal socket information"));

        String path = "/api/projects/" + REFERENCE_ID + "/docker/containers";
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(path)).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(
                        "Docker bilgileri alınamadı. Docker Engine'i ve yerel bağlantı ayarını kontrol edin."));
    }

    private void mockDockerInspect(String id, String response) throws Exception {
        InspectContainerCmd command = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd(id)).thenReturn(command);
        when(command.exec()).thenReturn(DockerClientConfig.getDefaultObjectMapper()
                .readValue(response, InspectContainerResponse.class));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void nonAdminCannotManageProfiles() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isForbidden());
        mvc.perform(get("/api/projects/" + REFERENCE_ID + "/git")).andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/projects")
                                .with(validCsrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(project("Blocked", "[]", true)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void gitHistoryReturnsDtoAndValidatesPageAndProject() throws Exception {
        HttpResponse<String> branches = gitHubResponse(200, "[{\"name\":\"feature/git\"}]");
        HttpResponse<String> commits = gitHubResponse(200, """
                        [{"sha":"0123456789012345678901234567890123456789",
                          "commit":{"message":"Git ekranı","author":{"name":"Example"},
                          "committer":{"date":"2026-11-13T10:00:00Z"}}}]
                        """);
        when(gitHubHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(branches, commits);

        mvc.perform(get("/api/projects/" + REFERENCE_ID + "/git").param("branch", "feature/git"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedBranch").value("feature/git"))
                .andExpect(jsonPath("$.branches[0]").value("feature/git"))
                .andExpect(jsonPath("$.commits[0].sha").value("0123456789012345678901234567890123456789"))
                .andExpect(jsonPath("$.commits[0].message").value("Git ekranı"))
                .andExpect(jsonPath("$.hasMoreCommits").value(false))
                .andExpect(jsonPath("$.fetchedAt").isNotEmpty());
        mvc.perform(get("/api/projects/" + REFERENCE_ID + "/git").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Sayfa numarası en az 1 olmalıdır."));
        mvc.perform(get("/api/projects/00000000-0000-0000-0000-000000000000/git"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void gitHubAccessFailureDoesNotBecomeAnOpsBridgeLoginFailure() throws Exception {
        HttpResponse<String> errorResponse = gitHubResponse(401, "private detail");
        when(gitHubHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(errorResponse);

        mvc.perform(get("/api/projects/" + REFERENCE_ID + "/git"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(
                        "GitHub token'ı geçersiz veya süresi dolmuş. Proje formundan bağlantının tokenını yenileyin."));
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> gitHubResponse(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
        return response;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidUrlsBlankNamesAndUnknownCapabilitiesAreRejected() throws Exception {
        mvc.perform(
                        post("/api/projects")
                                .with(validCsrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(project(" ", "[]", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());
        mvc.perform(
                        post("/api/projects")
                                .with(validCsrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        project("BadUrl", "[]", true)
                                                .replace(
                                                        "https://example.com/repo",
                                                        "javascript:alert(1)")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.repositoryUrl").exists());
        mvc.perform(
                        post("/api/projects")
                                .with(validCsrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        project("CredentialsInUrl", "[]", true)
                                                .replace(
                                                        "https://example.com/repo",
                                                        "https://user:password@example.com/repo")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/projects")
                                .with(validCsrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(project("BadCapability", "[\"DOES_NOT_EXIST\"]", true)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void integrationStatesFollowCapabilitiesAndProfileEnabledFlag() throws Exception {
        MvcResult created =
                mvc.perform(
                                post("/api/projects")
                                        .with(validCsrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(project("Minimal", "[]", true)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
        String path = "/api/projects/" + id;
        mvc.perform(get(path + "/integrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("NOT_SUPPORTED"))
                .andExpect(jsonPath("$[1].state").value("NOT_SUPPORTED"))
                .andExpect(jsonPath("$[2].state").value("NOT_SUPPORTED"));
        mvc.perform(
                        put(path)
                                .with(validCsrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(project("Minimal", "[\"BUILD_TRIGGER\"]", true)))
                .andExpect(status().isOk());
        mvc.perform(get(path + "/integrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("NOT_CONNECTED"))
                .andExpect(jsonPath("$[1].state").value("NOT_SUPPORTED"))
                .andExpect(jsonPath("$[2].state").value("NOT_SUPPORTED"));
        mvc.perform(
                        put(path)
                                .with(validCsrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(project("Minimal", "[\"BUILD_TRIGGER\"]", false)))
                .andExpect(status().isOk());
        mvc.perform(get(path + "/integrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value("DISABLED"))
                .andExpect(jsonPath("$[1].state").value("DISABLED"))
                .andExpect(jsonPath("$[2].state").value("DISABLED"));
        mvc.perform(get(path + "/docker/containers")).andExpect(status().isBadRequest());
        mvc.perform(delete(path).with(validCsrf())).andExpect(status().isNoContent());
        mvc.perform(get(path + "/integrations")).andExpect(status().isNotFound());
    }

    private Cookie login() throws Exception {
        MvcResult initial = csrfToken();
        JsonNode token = json.readTree(initial.getResponse().getContentAsString());
        Cookie csrfCookie = initial.getResponse().getCookie("OPSBRIDGE_CSRF");
        MvcResult result =
                mvc.perform(
                                post("/api/auth/login")
                                        .cookie(csrfCookie)
                                        .header(
                                                token.get("headerName").asText(),
                                                token.get("token").asText())
                                        .param("username", "admin")
                                        .param("password", "integration-test-only"))
                        .andExpect(status().isNoContent())
                        .andExpect(cookie().httpOnly("OPSBRIDGE_AUTH", true))
                        .andExpect(cookie().sameSite("OPSBRIDGE_AUTH", "Strict"))
                        .andExpect(cookie().path("OPSBRIDGE_AUTH", "/"))
                        .andExpect(cookie().maxAge("OPSBRIDGE_AUTH", 1800))
                        .andExpect(cookie().maxAge("OPSBRIDGE_CSRF", 0))
                        .andExpect(cookie().doesNotExist("JSESSIONID"))
                        .andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        return result.getResponse().getCookie("OPSBRIDGE_AUTH");
    }

    private MvcResult csrfToken() throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("OPSBRIDGE_CSRF", true))
                .andExpect(cookie().sameSite("OPSBRIDGE_CSRF", "Strict"))
                .andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        return result;
    }

    private RequestPostProcessor validCsrf() throws Exception {
        MvcResult result = csrfToken();
        Cookie csrfCookie = result.getResponse().getCookie("OPSBRIDGE_CSRF");
        JsonNode token = json.readTree(result.getResponse().getContentAsString());
        return request -> {
            Cookie[] existing = request.getCookies();
            Cookie[] cookies = existing == null ? new Cookie[1] : Arrays.copyOf(existing, existing.length + 1);
            cookies[cookies.length - 1] = csrfCookie;
            request.setCookies(cookies);
            request.addHeader(token.get("headerName").asText(), token.get("token").asText());
            return request;
        };
    }

    private String project(String name, String capabilities, boolean enabled) {
        return """
        {"name":"%s","repositoryUrl":"https://example.com/repo","environment":"local",
         "jenkinsBaseUrlRef":null,"jenkinsJobPath":null,"containerLabelSelector":null,
         "healthUrl":null,"capabilities":%s,"enabled":%s}
        """
                .formatted(name, capabilities, enabled);
    }
}
