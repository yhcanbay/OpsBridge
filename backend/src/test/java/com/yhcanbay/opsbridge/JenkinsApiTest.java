package com.yhcanbay.opsbridge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.yhcanbay.opsbridge.dto.response.JenkinsBuildResponse;
import com.yhcanbay.opsbridge.dto.response.JenkinsJobResponse;
import com.yhcanbay.opsbridge.enums.IntegrationState;
import com.yhcanbay.opsbridge.exception.JenkinsException;
import com.yhcanbay.opsbridge.service.IJenkinsService;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JenkinsApiTest {

    private static final String PROJECT_ID = "b946c162-581a-46ec-a961-066d69c974f1";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockitoBean private IJenkinsService jenkinsService;

    @Test
    void anonymousCannotReadJenkinsDetails() throws Exception {
        mvc.perform(get("/api/projects/" + PROJECT_ID + "/jenkins"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void onlyAdminCanReadJenkinsDetails() throws Exception {
        mvc.perform(get("/api/projects/" + PROJECT_ID + "/jenkins"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void returnsSelectedBuildAndSuccessfulReadTime() throws Exception {
        JenkinsJobResponse response = JenkinsJobResponse.builder()
                .jobName("task-app")
                .jobUrl("http://jenkins.test/job/task-app/")
                .selectedBuild(JenkinsBuildResponse.builder().number(47).result("FAILURE").build())
                .builds(List.of())
                .stages(List.of())
                .fetchedAt(Instant.parse("2026-11-14T10:00:00Z"))
                .build();
        when(jenkinsService.getJenkinsJobInfo(UUID.fromString(PROJECT_ID), 47L)).thenReturn(response);
        mvc.perform(get("/api/projects/" + PROJECT_ID + "/jenkins").param("build", "47"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobName").value("task-app"))
                .andExpect(jsonPath("$.selectedBuild.number").value(47))
                .andExpect(jsonPath("$.fetchedAt").value("2026-11-14T10:00:00Z"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void jenkinsAuthenticationFailureDoesNotBecomeAnOpsBridge401() throws Exception {
        when(jenkinsService.getJenkinsJobInfo(any(UUID.class), isNull())).thenThrow(
                new JenkinsException(IntegrationState.ACCESS_DENIED, "Jenkins erişimi reddedildi."));
        mvc.perform(get("/api/projects/" + PROJECT_ID + "/jenkins"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.jenkinsState").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.detail").value("Jenkins erişimi reddedildi."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void existingProfileFieldsCanBeCorrectedAndArePersistedWithoutANewTable() throws Exception {
        String body = profile("http://jenkins.test:8081", "wrong-job");
        String result = mvc.perform(post("/api/projects").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = json.readTree(result).path("id").asString();
        mvc.perform(put("/api/projects/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(profile("http://correct.test:8081/jenkins", "apps/correct-job")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jenkinsBaseUrlRef").value("http://correct.test:8081/jenkins"))
                .andExpect(jsonPath("$.jenkinsJobPath").value("apps/correct-job"));
        mvc.perform(get("/api/projects/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jenkinsJobPath").value("apps/correct-job"));
        mvc.perform(put("/api/projects/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void credentialsInsideTheProjectUrlAreRejectedAsAFieldError() throws Exception {
        mvc.perform(post("/api/projects").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(profile("http://user:secret@jenkins.test", "task-app")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.jenkinsBaseUrlRef").exists());
    }

    private String profile(String url, String path) {
        return """
                {"name":"Jenkins test project","repositoryUrl":"https://github.com/example/task-app",
                 "environment":"local","jenkinsBaseUrlRef":"%s","jenkinsJobPath":"%s",
                 "capabilities":["BUILD_VIEW"],"enabled":true}
                """.formatted(url, path);
    }
}
