package com.yhcanbay.opsbridge.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.yhcanbay.opsbridge.dto.response.GitCommitResponse;
import com.yhcanbay.opsbridge.dto.response.GitHistoryResponse;
import com.yhcanbay.opsbridge.entity.ProjectProfile;
import com.yhcanbay.opsbridge.exception.GitHubException;
import com.yhcanbay.opsbridge.exception.GitRequestException;
import com.yhcanbay.opsbridge.exception.ProjectNotFoundException;
import com.yhcanbay.opsbridge.repository.ProjectRepository;
import com.yhcanbay.opsbridge.service.IGitService;
import com.yhcanbay.opsbridge.service.IGitHubConnectionService;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class GitServiceImpl implements IGitService {

    private static final int PAGE_SIZE = 20;

    private final ProjectRepository projectRepository;
    private final HttpClient gitHubHttpClient;
    private final ObjectMapper objectMapper;
    private final IGitHubConnectionService connectionService;

    @Override
    public void validateRepositoryAccess(String repositoryUrl, UUID connectionId) {
        String repositoryPath = getRepositoryPath(repositoryUrl);
        String token = connectionService.getToken(connectionId);
        HttpResponse<String> response = sendGitHubRequest(
                "https://api.github.com/repos/" + repositoryPath + "/branches?per_page=1", token);
        readArray(response.body());
    }

    @Override
    public GitHistoryResponse getGitHistory(UUID projectId, String branch, int page, int branchPage) {
        if (page < 1 || branchPage < 1) {
            throw new GitRequestException("Sayfa numarası en az 1 olmalıdır.");
        }

        Optional<ProjectProfile> optionalProject = projectRepository.findById(projectId);
        if (optionalProject.isEmpty()) {
            throw new ProjectNotFoundException(projectId);
        }

        ProjectProfile project = optionalProject.get();
        if (!project.isEnabled()) {
            throw new GitRequestException("Proje pasif. GitHub geçmişini görmek için profili aktifleştirin.");
        }

        String repositoryPath = getRepositoryPath(project.getRepositoryUrl());
        String apiUrl = "https://api.github.com/repos/" + repositoryPath;
        String gitHubToken = null;
        if (project.getGithubConnectionId() != null) {
            gitHubToken = connectionService.getToken(project.getGithubConnectionId());
        }

        HttpResponse<String> branchResponse = sendGitHubRequest(
                apiUrl + "/branches?per_page=" + PAGE_SIZE + "&page=" + branchPage, gitHubToken);
        JsonNode branchNodes = readArray(branchResponse.body());
        List<String> branches = new ArrayList<>();

        for (JsonNode branchNode : branchNodes) {
            branches.add(branchNode.path("name").asString());
        }

        String selectedBranch = branch;
        if (selectedBranch.isBlank() && !branches.isEmpty()) {
            selectedBranch = branches.get(0);
        }

        GitHistoryResponse result = GitHistoryResponse.builder()
                .branches(branches)
                .branchPage(branchPage)
                .hasMoreBranches(hasNextPage(branchResponse))
                .selectedBranch(selectedBranch)
                .commits(new ArrayList<>())
                .page(page)
                .build();

        if (!selectedBranch.isBlank()) {
            String encodedBranch = URLEncoder.encode(selectedBranch, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            HttpResponse<String> commitResponse = sendGitHubRequest(
                    apiUrl + "/commits?sha=" + encodedBranch + "&per_page=" + PAGE_SIZE + "&page=" + page, gitHubToken);
            JsonNode commitNodes = readArray(commitResponse.body());
            List<GitCommitResponse> commits = new ArrayList<>();

            for (JsonNode commitNode : commitNodes) {
                JsonNode commit = commitNode.path("commit");
                GitCommitResponse dtoCommit = GitCommitResponse.builder()
                        .sha(commitNode.path("sha").asString())
                        .message(commit.path("message").asString())
                        .author(commit.path("author").path("name").asString("Bilinmiyor"))
                        .committedAt(commit.path("committer").path("date").asString(""))
                        .build();
                commits.add(dtoCommit);
            }

            result.setCommits(commits);
            result.setHasMoreCommits(hasNextPage(commitResponse));
        }

        result.setFetchedAt(Instant.now());
        return result;
    }

    private String getRepositoryPath(String repositoryUrl) {
        URI repositoryUri = URI.create(repositoryUrl);
        String path = repositoryUri.getPath();
        boolean validAddress = "https".equalsIgnoreCase(repositoryUri.getScheme())
                && "github.com".equalsIgnoreCase(repositoryUri.getHost())
                && repositoryUri.getPort() == -1
                && repositoryUri.getUserInfo() == null
                && repositoryUri.getQuery() == null
                && repositoryUri.getFragment() == null
                && path != null
                && path.matches("/[A-Za-z0-9-]+/[A-Za-z0-9_.-]+/?");

        if (!validAddress) {
            throw new GitRequestException("GitHub için https://github.com/kullanici/depo biçiminde bir repository adresi girin.");
        }

        String repositoryPath = path.substring(1);
        if (repositoryPath.endsWith("/")) {
            repositoryPath = repositoryPath.substring(0, repositoryPath.length() - 1);
        }
        if (repositoryPath.endsWith(".git")) {
            repositoryPath = repositoryPath.substring(0, repositoryPath.length() - 4);
        }

        String repositoryName = repositoryPath.substring(repositoryPath.indexOf('/') + 1);
        if (repositoryName.isBlank() || repositoryName.equals(".") || repositoryName.equals("..")) {
            throw new GitRequestException("GitHub repository adı geçersiz.");
        }

        return repositoryPath;
    }

    private HttpResponse<String> sendGitHubRequest(String url, String gitHubToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2026-03-10")
                .header("User-Agent", "OpsBridge")
                .GET();

        if (gitHubToken != null && !gitHubToken.isBlank()) {
            try {
                builder.header("Authorization", "Bearer " + gitHubToken);
            } catch (IllegalArgumentException exception) {
                throw new GitHubException("GitHub token biçimi geçersiz. Proje formundan bağlantının tokenını yenileyin.");
            }
        }

        try {
            HttpResponse<String> response = gitHubHttpClient.send(
                    builder.build(), HttpResponse.BodyHandlers.ofString());
            checkGitHubStatus(response);
            return response;
        } catch (HttpTimeoutException exception) {
            throw new GitHubException("GitHub isteği zaman aşımına uğradı. Tekrar deneyin.");
        } catch (IOException exception) {
            throw new GitHubException("GitHub bağlantısı kurulamadı. Sunucunun internet bağlantısını kontrol edin.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GitHubException("GitHub isteği kesildi. Tekrar deneyin.");
        }
    }

    private void checkGitHubStatus(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status == 200) {
            return;
        }
        if (status == 401) {
            throw new GitHubException("GitHub token'ı geçersiz veya süresi dolmuş. Proje formundan bağlantının tokenını yenileyin.");
        }
        if (status == 429 || (status == 403
                && response.headers().firstValue("X-RateLimit-Remaining").orElse("").equals("0"))) {
            throw new GitHubException("GitHub istek sınırına ulaşıldı. Bir süre sonra tekrar deneyin.");
        }
        if (status == 403) {
            throw new GitHubException("GitHub erişimi reddetti. Token'ın repository okuma iznini kontrol edin; geçici istek sınırı da uygulanmış olabilir.");
        }
        if (status == 404) {
            throw new GitHubException("GitHub deposu veya branch bulunamadı. Özel depolarda token'ın erişim iznini de kontrol edin.");
        }
        if (status == 409) {
            throw new GitHubException("GitHub deposunda okunabilir commit geçmişi bulunmuyor.");
        }
        if (status == 422) {
            throw new GitHubException("GitHub seçilen branch veya sayfayı okuyamadı. Branch listesini yenileyin.");
        }
        if (status == 301 || status == 302 || status == 307 || status == 308) {
            throw new GitHubException("GitHub deposunun adresi değişmiş olabilir. Profildeki repository adresini güncelleyin.");
        }

        throw new GitHubException("GitHub isteği tamamlanamadı. HTTP durum kodu: " + status);
    }

    private JsonNode readArray(String body) {
        try {
            JsonNode nodes = objectMapper.readTree(body);
            if (nodes == null || !nodes.isArray()) {
                throw new GitHubException("GitHub beklenen liste yanıtını döndürmedi.");
            }
            return nodes;
        } catch (JacksonException exception) {
            throw new GitHubException("GitHub yanıtı okunamadı. Tekrar deneyin.");
        }
    }

    private boolean hasNextPage(HttpResponse<String> response) {
        String linkHeader = response.headers().firstValue("Link").orElse("");
        return linkHeader.contains("rel=\"next\"");
    }
}
