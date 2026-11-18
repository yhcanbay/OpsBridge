package com.yhcanbay.opsbridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import com.yhcanbay.opsbridge.dto.request.GitHubConnectionRequest;
import com.yhcanbay.opsbridge.exception.GitHubException;
import com.yhcanbay.opsbridge.service.IGitHubConnectionService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GitHubConnectionApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired IGitHubConnectionService connectionService;
    @Autowired EntityManager entityManager;
    @MockitoBean HttpClient gitHubHttpClient;

    @Test
    void anonymousCannotListConnections() throws Exception {
        mvc.perform(get("/api/github-connections")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void nonAdminCannotListConnections() throws Exception {
        mvc.perform(get("/api/github-connections")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void csrfIsRequiredForConnectionMutations() throws Exception {
        mvc.perform(post("/api/github-connections").contentType(MediaType.APPLICATION_JSON)
                .content(connectionRequest("Test", "private-token"))).andExpect(status().isForbidden());
        mvc.perform(put("/api/github-connections/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content(connectionRequest("Test", "private-token"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/github-connections/" + UUID.randomUUID())).andExpect(status().isForbidden());
        verifyNoInteractions(gitHubHttpClient);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void storesEncryptedTokensAndAllowsSeveralConnectionsForOneAccount() throws Exception {
        mockAccount(42, "example");
        String firstId = createConnection("TaskApp okuma", "private-first-token");
        createConnection("Backend okuma", "private-second-token");
        String encrypted = jdbc.queryForObject("SELECT encrypted_token FROM github_connections WHERE id = ?",
                String.class, UUID.fromString(firstId));
        assertThat(encrypted).doesNotContain("private-first-token");
        assertThat(connectionService.getToken(UUID.fromString(firstId))).isEqualTo("private-first-token");

        String body = mvc.perform(get("/api/github-connections"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("private-first-token", "private-second-token", encrypted, "encryptedToken");
        assertThat(json.readTree(body).get(0).size()).isEqualTo(3);
        GitHubConnectionRequest request = new GitHubConnectionRequest("Test", "private-first-token");
        assertThat(request.toString()).doesNotContain("private-first-token");
        assertThat(json.writeValueAsString(request)).doesNotContain("private-first-token");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void canRenameRotateAndDeleteWithoutReturningTheToken() throws Exception {
        mockAccount(42, "example");
        String id = createConnection("Eski ad", "old-private-token");
        clearInvocations(gitHubHttpClient);
        mvc.perform(put("/api/github-connections/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(connectionRequest("Yeni ad", ""))).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Yeni ad"));
        verifyNoInteractions(gitHubHttpClient);
        assertThat(connectionService.getToken(UUID.fromString(id))).isEqualTo("old-private-token");

        mockAccount(42, "renamed-account");
        mvc.perform(put("/api/github-connections/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(connectionRequest("Yeni ad", "new-private-token"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.accountLogin").value("renamed-account"))
                .andExpect(jsonPath("$.token").doesNotExist());
        assertThat(connectionService.getToken(UUID.fromString(id))).isEqualTo("new-private-token");

        mockAccount(99, "another-account");
        mvc.perform(put("/api/github-connections/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(connectionRequest("Başka hesap", "another-token"))).andExpect(status().isBadRequest());
        assertThat(connectionService.getToken(UUID.fromString(id))).isEqualTo("new-private-token");

        mvc.perform(delete("/api/github-connections/" + id).with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidTokenAndUpstreamFailureDoNotCreateConnectionsOrExposeDetails() throws Exception {
        HttpResponse<String> unauthorized = response(401, "private upstream body");
        when(gitHubHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(unauthorized);
        mvc.perform(post("/api/github-connections").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(connectionRequest("Test", "private-token"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/github-connections")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        when(gitHubHttpClient.<String>send(any(HttpRequest.class), any())).thenThrow(new IOException("private upstream detail"));
        String body = mvc.perform(post("/api/github-connections").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(connectionRequest("Test", "private-token"))).andExpect(status().isBadGateway())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("private-token", "private upstream");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsEmptyAndMalformedCredentialsBeforeSendingAnything() throws Exception {
        for (String token : new String[] {"", "private\ninvalid", "private token", "private-token-".repeat(200)}) {
            mvc.perform(post("/api/github-connections").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(connectionRequest("Test", token))).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/github-connections").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(connectionRequest(" ", "private-token"))).andExpect(status().isBadRequest());
        verifyNoInteractions(gitHubHttpClient);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eachProjectUsesItsSelectedTokenAndUsedConnectionsCannotBeDeleted() throws Exception {
        mockAccount(42, "example");
        String firstId = createConnection("Birinci", "first-project-token");
        String secondId = createConnection("İkinci", "second-project-token");
        List<HttpRequest> requests = new ArrayList<>();
        HttpResponse<String> branches = response(200, "[]");
        when(gitHubHttpClient.<String>send(any(HttpRequest.class), any())).thenAnswer(invocation -> {
            requests.add(invocation.getArgument(0));
            return branches;
        });

        String firstProject = createProject("First", firstId);
        String secondProject = createProject("Second", secondId);
        mvc.perform(get("/api/projects/" + firstProject + "/git")).andExpect(status().isOk());
        mvc.perform(get("/api/projects/" + secondProject + "/git")).andExpect(status().isOk());
        assertThat(requests).hasSize(4);
        assertThat(requests.get(0).headers().firstValue("Authorization")).contains("Bearer first-project-token");
        assertThat(requests.get(1).headers().firstValue("Authorization")).contains("Bearer second-project-token");
        assertThat(requests.get(2).headers().firstValue("Authorization")).contains("Bearer first-project-token");
        assertThat(requests.get(3).headers().firstValue("Authorization")).contains("Bearer second-project-token");

        clearInvocations(gitHubHttpClient);
        mvc.perform(put("/api/projects/" + firstProject).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(projectRequest("Renamed", firstId))).andExpect(status().isOk())
                .andExpect(jsonPath("$.githubConnectionId").value(firstId));
        verifyNoInteractions(gitHubHttpClient);
        mvc.perform(delete("/api/github-connections/" + firstId).with(csrf())).andExpect(status().isConflict());
        mvc.perform(put("/api/projects/" + firstProject).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(projectRequest("Renamed", null))).andExpect(status().isOk());
        mvc.perform(delete("/api/github-connections/" + firstId).with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void permissionFailureDoesNotSaveTheProjectAndDoesNotFallBackToAnonymous() throws Exception {
        mockAccount(42, "example");
        String id = createConnection("Restricted", "private-token");
        HttpResponse<String> denied = response(404, "private upstream detail");
        when(gitHubHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(denied);
        mvc.perform(post("/api/projects").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(projectRequest("Rejected", id))).andExpect(status().isBadGateway());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM project_profiles WHERE name = 'Rejected'", Long.class)).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void corruptedCiphertextProducesASafeError() throws Exception {
        mockAccount(42, "example");
        String id = createConnection("Corrupt", "private-token");
        jdbc.update("UPDATE github_connections SET encrypted_token = ? WHERE id = ?", "invalid-base64!", UUID.fromString(id));
        entityManager.clear();
        assertThatThrownBy(() -> connectionService.getToken(UUID.fromString(id)))
                .isInstanceOf(GitHubException.class).hasMessageNotContaining("invalid-base64!");
    }

    private void mockAccount(long id, String login) throws Exception {
        HttpResponse<String> account = response(200, "{\"id\":" + id + ",\"login\":\"" + login + "\"}");
        when(gitHubHttpClient.<String>send(any(HttpRequest.class), any())).thenReturn(account);
    }

    private String createConnection(String name, String token) throws Exception {
        String body = mvc.perform(post("/api/github-connections").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(connectionRequest(name, token))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountLogin").value("example"))
                .andExpect(jsonPath("$.token").doesNotExist()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("id").asString();
    }

    private String connectionRequest(String name, String token) {
        return json.writeValueAsString(Map.of("name", name, "token", token));
    }

    private String createProject(String name, String connectionId) throws Exception {
        String body = mvc.perform(post("/api/projects").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(projectRequest(name, connectionId))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("id").asString();
    }

    private String projectRequest(String name, String connectionId) {
        return "{\"name\":\"" + name + "\",\"repositoryUrl\":\"https://github.com/example/task-app\","
                + "\"environment\":\"test\",\"capabilities\":[],\"enabled\":true,\"githubConnectionId\":"
                + (connectionId == null ? "null" : "\"" + connectionId + "\"") + "}";
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of(), (name, value) -> true));
        return response;
    }
}
