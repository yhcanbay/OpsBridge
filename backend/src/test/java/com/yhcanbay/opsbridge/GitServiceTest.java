package com.yhcanbay.opsbridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.yhcanbay.opsbridge.dto.response.GitHistoryResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.exception.GitHubException;
import com.yhcanbay.opsbridge.exception.ProjectNotFoundException;
import com.yhcanbay.opsbridge.repository.ProjectRepository;
import com.yhcanbay.opsbridge.service.impl.GitServiceImpl;

import tools.jackson.databind.ObjectMapper;

class GitServiceTest {

    private final UUID projectId = UUID.randomUUID();
    private ProjectRepository projectRepository;
    private HttpClient httpClient;
    private GitServiceImpl gitService;
    private ProjectProfile project;

    @BeforeEach
    void setUp() {
        projectRepository = mock(ProjectRepository.class);
        httpClient = mock(HttpClient.class);
        gitService = new GitServiceImpl(projectRepository, httpClient, new ObjectMapper());

        project = new ProjectProfile();
        project.setRepositoryUrl("https://github.com/example/task-app.git/");
        project.setEnabled(true);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    }

    @Test
    void readsSelectedBranchHistoryAndPaginationWithoutExposingToken() throws Exception {
        ReflectionTestUtils.setField(gitService, "gitHubToken", "test-only-token");
        String nextLink = "<https://api.github.com/repos/example/task-app/branches?page=3>; rel=\"next\"";
        HttpResponse<String> branches = response(200, "[{\"name\":\"feature/git & ui\"}]",
                Map.of("Link", List.of(nextLink)));
        HttpResponse<String> commits = response(200, """
                [{
                    "sha": "0123456789012345678901234567890123456789",
                    "commit": {
                        "message": "Git görünümü\\n\\nAyrıntılar",
                        "author": {"name": "Example"},
                        "committer": {"date": "2026-11-13T10:00:00Z"}
                    }
                }]
                """, Map.of());
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(branches, commits);

        GitHistoryResponse result = gitService.getGitHistory(projectId, "feature/git & ui", 3, 2);

        assertThat(result.getSelectedBranch()).isEqualTo("feature/git & ui");
        assertThat(result.getBranchPage()).isEqualTo(2);
        assertThat(result.isHasMoreBranches()).isTrue();
        assertThat(result.getPage()).isEqualTo(3);
        assertThat(result.isHasMoreCommits()).isFalse();
        assertThat(result.getFetchedAt()).isNotNull();
        assertThat(result.getCommits()).hasSize(1);
        assertThat(result.getCommits().getFirst().getSha()).hasSize(40);
        assertThat(result.getCommits().getFirst().getMessage()).isEqualTo("Git görünümü\n\nAyrıntılar");
        assertThat(result.getCommits().getFirst().getAuthor()).isEqualTo("Example");
        assertThat(result.getCommits().getFirst().getCommittedAt()).isEqualTo("2026-11-13T10:00:00Z");
        assertThat(new ObjectMapper().writeValueAsString(result)).doesNotContain("test-only-token");

        ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requests.capture(), any());
        assertThat(requests.getAllValues().get(0).uri().toString()).isEqualTo(
                "https://api.github.com/repos/example/task-app/branches?per_page=20&page=2");
        HttpRequest commitRequest = requests.getAllValues().get(1);
        assertThat(commitRequest.uri().toString()).isEqualTo(
                "https://api.github.com/repos/example/task-app/commits?sha=feature%2Fgit%20%26%20ui&per_page=20&page=3");
        assertThat(commitRequest.headers().firstValue("Authorization")).contains("Bearer test-only-token");
        assertThat(commitRequest.method()).isEqualTo("GET");
    }

    @Test
    void selectsFirstReturnedBranchAndUsesGitHubNextLinkForCommitPaging() throws Exception {
        HttpResponse<String> branches = response(200, "[{\"name\":\"develop\"},{\"name\":\"main\"}]", Map.of());
        HttpResponse<String> commits = response(200, "[]", Map.of(
                "Link", List.of("<https://api.github.com/next>; rel=\"next\"")));
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(branches, commits);

        GitHistoryResponse result = gitService.getGitHistory(projectId, "", 1, 1);

        assertThat(result.getBranches()).containsExactly("develop", "main");
        assertThat(result.getSelectedBranch()).isEqualTo("develop");
        assertThat(result.isHasMoreCommits()).isTrue();
        ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requests.capture(), any());
        assertThat(requests.getAllValues().get(0).headers().firstValue("Authorization")).isEmpty();
    }

    @Test
    void emptyRepositoryDoesNotRequestCommitsWithoutABranch() throws Exception {
        HttpResponse<String> emptyResponse = response(200, "[]", Map.of());
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(emptyResponse);

        GitHistoryResponse result = gitService.getGitHistory(projectId, "", 1, 1);

        assertThat(result.getBranches()).isEmpty();
        assertThat(result.getCommits()).isEmpty();
        assertThat(result.getFetchedAt()).isNotNull();
        verify(httpClient).send(any(HttpRequest.class), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com/owner/repo", "http://github.com/owner/repo",
            "https://github.com.evil.test/owner/repo", "https://github.com/owner/repo/issues",
            "https://user:password@github.com/owner/repo", "https://github.com/owner/repo?next=other",
            "https://github.com/owner/..", "https://github.com/owner/.git",
            "https://github.com/owner/%2F", "https://github.com:443/owner/repo"
    })
    void rejectsUnsupportedRepositoryAddressesBeforeSendingRequests(String repositoryUrl) {
        project.setRepositoryUrl(repositoryUrl);
        assertThatThrownBy(() -> gitService.getGitHistory(projectId, "", 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(httpClient);
    }

    @Test
    void missingAndDisabledProjectsDoNotSendRequests() {
        project.setEnabled(false);
        assertThatThrownBy(() -> gitService.getGitHistory(projectId, "", 1, 1))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("pasif");

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> gitService.getGitHistory(projectId, "", 1, 1))
                .isInstanceOf(ProjectNotFoundException.class);
        verifyNoInteractions(httpClient);
    }

    @Test
    void invalidPagesAreRejectedBeforeReadingTheProject() {
        assertThatThrownBy(() -> gitService.getGitHistory(projectId, "", 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gitService.getGitHistory(projectId, "", 1, -1))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(projectRepository, httpClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 401, 403, 404, 409, 422, 429, 500})
    void gitHubErrorsDoNotReturnSuccessOrRawUpstreamBodies(int status) throws Exception {
        HttpResponse<String> errorResponse = response(status, "private upstream detail", Map.of());
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(errorResponse);

        assertThatThrownBy(() -> gitService.getGitHistory(projectId, "", 1, 1))
                .isInstanceOf(GitHubException.class).hasMessageNotContaining("private upstream detail");
        verify(httpClient).send(any(HttpRequest.class), any());
    }

    @Test
    void reportsRateLimitFromResponseHeaders() throws Exception {
        HttpResponse<String> limitResponse = response(403, "{}",
                Map.of("X-RateLimit-Remaining", List.of("0")));
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(limitResponse);

        assertThatThrownBy(() -> gitService.getGitHistory(projectId, "", 1, 1))
                .isInstanceOf(GitHubException.class).hasMessageContaining("istek sınırına");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not JSON", "{}", "null", ""})
    void invalidGitHubJsonDoesNotLookLikeAnEmptyHistory(String body) throws Exception {
        HttpResponse<String> invalidResponse = response(200, body, Map.of());
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(invalidResponse);

        assertThatThrownBy(() -> gitService.getGitHistory(projectId, "", 1, 1))
                .isInstanceOf(GitHubException.class);
    }

    @Test
    void connectionFailuresAndTimeoutsHaveReadableMessages() throws Exception {
        when(httpClient.<String>send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("private detail"))
                .thenThrow(new HttpTimeoutException("private detail"));

        assertThatThrownBy(() -> gitService.getGitHistory(projectId, "", 1, 1))
                .isInstanceOf(GitHubException.class).hasMessageContaining("bağlantısı kurulamadı");
        assertThatThrownBy(() -> gitService.getGitHistory(projectId, "", 1, 1))
                .isInstanceOf(GitHubException.class).hasMessageContaining("zaman aşımına");
    }

    @Test
    void invalidTokenDoesNotLeakIntoErrorMessages() {
        ReflectionTestUtils.setField(gitService, "gitHubToken", "private-token\ninvalid-header");

        assertThatThrownBy(() -> gitService.getGitHistory(projectId, "", 1, 1))
                .isInstanceOf(GitHubException.class)
                .hasMessageContaining("token biçimi geçersiz")
                .hasMessageNotContaining("private-token");
        verifyNoInteractions(httpClient);
    }

    @Test
    void interruptedRequestPreservesThreadInterruption() throws Exception {
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenThrow(new InterruptedException());
        try {
            assertThatThrownBy(() -> gitService.getGitHistory(projectId, "", 1, 1))
                    .isInstanceOf(GitHubException.class).hasMessageContaining("kesildi");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body, Map<String, List<String>> headers) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (name, value) -> true));
        return response;
    }
}
