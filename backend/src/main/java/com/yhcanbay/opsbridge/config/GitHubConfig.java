package com.yhcanbay.opsbridge.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.AesGcmBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;

@Configuration
public class GitHubConfig {

    @Bean
    BytesEncryptor gitHubTokenEncryptor(@Value("${opsbridge.github.encryption-key}") String encodedKey) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("OPSBRIDGE_TOKEN_ENCRYPTION_KEY, Base64 biçiminde 32 baytlık bir anahtar olmalıdır.");
        }
        if (key.length != 32) {
            throw new IllegalStateException("OPSBRIDGE_TOKEN_ENCRYPTION_KEY, Base64 biçiminde 32 baytlık bir anahtar olmalıdır.");
        }

        return AesGcmBytesEncryptor.withSecretKey(new SecretKeySpec(key, "AES")).build();
    }

    @Bean
    HttpClient gitHubHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }
}
