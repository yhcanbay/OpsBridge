package com.yhcanbay.opsbridge.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yhcanbay.opsbridge.dto.request.GitHubConnectionRequest;
import com.yhcanbay.opsbridge.dto.response.GitHubConnectionResponse;
import com.yhcanbay.opsbridge.entity.GitHubConnection;
import com.yhcanbay.opsbridge.exception.GitHubConnectionInUseException;
import com.yhcanbay.opsbridge.exception.GitHubConnectionNotFoundException;
import com.yhcanbay.opsbridge.exception.GitHubException;
import com.yhcanbay.opsbridge.exception.GitRequestException;
import com.yhcanbay.opsbridge.mapper.GitHubConnectionMapper;
import com.yhcanbay.opsbridge.repository.GitHubConnectionRepository;
import com.yhcanbay.opsbridge.repository.ProjectRepository;
import com.yhcanbay.opsbridge.service.IGitHubConnectionService;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GitHubConnectionServiceImpl implements IGitHubConnectionService {

    private final GitHubConnectionRepository connectionRepository;
    private final ProjectRepository projectRepository;
    private final GitHubConnectionMapper connectionMapper;
    private final BytesEncryptor gitHubTokenEncryptor;
    private final HttpClient gitHubHttpClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<GitHubConnectionResponse> getAllConnections() {
        List<GitHubConnection> connections = connectionRepository.findAllByOrderByAccountLoginAscNameAsc();
        List<GitHubConnectionResponse> responses = new ArrayList<>();

        for (GitHubConnection connection : connections) {
            responses.add(connectionMapper.toDto(connection));
        }
        return responses;
    }

    @Override
    @Transactional
    public GitHubConnectionResponse saveConnection(GitHubConnectionRequest request) {
        String token = normalizeToken(request.getToken());
        if (token.isBlank()) {
            throw new GitRequestException("Yeni bağlantı için GitHub tokenı girin.");
        }

        JsonNode account = getGitHubAccount(token);
        GitHubConnection connection = new GitHubConnection();
        connection.setName(request.getName().trim());
        connection.setGithubUserId(account.path("id").asLong());
        connection.setAccountLogin(account.path("login").asString());
        connection.setEncryptedToken(encryptToken(token));

        GitHubConnection savedConnection = connectionRepository.saveAndFlush(connection);
        return connectionMapper.toDto(savedConnection);
    }

    @Override
    @Transactional
    public GitHubConnectionResponse updateConnection(UUID connectionId, GitHubConnectionRequest request) {
        GitHubConnection connection = findConnectionById(connectionId);
        String token = normalizeToken(request.getToken());

        if (!token.isBlank()) {
            JsonNode account = getGitHubAccount(token);
            if (account.path("id").asLong() != connection.getGithubUserId()) {
                throw new GitRequestException("Yeni token aynı GitHub hesabına ait olmalıdır. Farklı hesap için yeni bağlantı ekleyin.");
            }
            connection.setAccountLogin(account.path("login").asString());
            connection.setEncryptedToken(encryptToken(token));
        }

        connection.setName(request.getName().trim());
        GitHubConnection updatedConnection = connectionRepository.saveAndFlush(connection);
        return connectionMapper.toDto(updatedConnection);
    }

    @Override
    @Transactional
    public void deleteConnection(UUID connectionId) {
        GitHubConnection connection = findConnectionById(connectionId);
        if (projectRepository.existsByGithubConnectionId(connectionId)) {
            throw new GitHubConnectionInUseException();
        }

        try {
            connectionRepository.delete(connection);
            connectionRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            // Kontrolün hemen ardından başka bir proje bağlantıyı seçmiş olabilir.
            throw new GitHubConnectionInUseException();
        }
    }

    @Override
    public String getToken(UUID connectionId) {
        GitHubConnection connection = findConnectionById(connectionId);
        try {
            byte[] encryptedToken = Base64.getDecoder().decode(connection.getEncryptedToken());
            byte[] token = gitHubTokenEncryptor.decrypt(encryptedToken);
            return new String(token, StandardCharsets.UTF_8);
        } catch (RuntimeException exception) {
            throw new GitHubException("GitHub bağlantısının tokenı okunamadı. Sunucunun şifreleme anahtarını kontrol edin.");
        }
    }

    private GitHubConnection findConnectionById(UUID connectionId) {
        Optional<GitHubConnection> optionalConnection = connectionRepository.findById(connectionId);
        if (optionalConnection.isEmpty()) {
            throw new GitHubConnectionNotFoundException();
        }
        return optionalConnection.get();
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        String token = value.trim();
        if (token.length() > 2048) {
            throw new GitRequestException("GitHub tokenı en fazla 2048 karakter olabilir.");
        }
        for (int index = 0; index < token.length(); index++) {
            char character = token.charAt(index);
            if (character <= 32 || character >= 127) {
                throw new GitRequestException("GitHub token biçimi geçersiz. Token içinde boşluk veya kontrol karakteri bulunamaz.");
            }
        }
        return token;
    }

    private String encryptToken(String token) {
        byte[] encryptedToken = gitHubTokenEncryptor.encrypt(token.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedToken);
    }

    private JsonNode getGitHubAccount(String token) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/user"))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2026-03-10")
                .header("User-Agent", "OpsBridge")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        try {
            HttpResponse<String> response = gitHubHttpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 401) {
                throw new GitRequestException("GitHub tokenı geçersiz veya süresi dolmuş. Yeni bir token girin.");
            }
            if (status != 200) {
                throw new GitHubException("GitHub hesabı doğrulanamadı. Token izinlerini veya GitHub istek sınırını kontrol edin. HTTP: " + status);
            }

            JsonNode account = objectMapper.readTree(response.body());
            if (account == null || !account.isObject() || !account.path("id").isIntegralNumber()
                    || account.path("id").asLong() <= 0 || !account.path("login").isString()
                    || !account.path("login").asString().matches("[A-Za-z0-9-]{1,100}")) {
                throw new GitHubException("GitHub geçerli bir hesap bilgisi döndürmedi.");
            }
            return account;
        } catch (JacksonException exception) {
            throw new GitHubException("GitHub hesap yanıtı okunamadı. Tekrar deneyin.");
        } catch (IOException exception) {
            throw new GitHubException("GitHub hesabına bağlanılamadı veya istek zaman aşımına uğradı. Tekrar deneyin.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GitHubException("GitHub hesap doğrulaması kesildi. Tekrar deneyin.");
        }
    }
}
