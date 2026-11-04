package com.yhcanbay.opsbridge.dto.request;

import com.yhcanbay.opsbridge.enums.Capability;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.net.URI;
import java.util.Set;

public record ProjectRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 500) String repositoryUrl,
        @NotBlank @Pattern(regexp = "[a-z][a-z0-9_-]{0,29}") String environment,
        @Size(max = 100) String jenkinsBaseUrlRef,
        @Size(max = 200) String jenkinsJobPath,
        @Size(max = 250) String containerLabelSelector,
        @Size(max = 500) String healthUrl,
        @NotNull Set<@NotNull Capability> capabilities,
        @NotNull Boolean enabled) {

    @AssertTrue(message = "Repository adresi, kimlik bilgisi içermeyen bir HTTP(S) URL olmalıdır.")
    public boolean isRepositoryUrlValid() {
        return isHttpUrlWithoutCredentials(repositoryUrl);
    }

    @AssertTrue(
            message = "Health adresi boş veya kimlik bilgisi içermeyen bir HTTP(S) URL olmalıdır.")
    public boolean isHealthUrlValid() {
        if (healthUrl == null || healthUrl.isBlank()) {
            return true;
        }

        return isHttpUrlWithoutCredentials(healthUrl);
    }

    private static boolean isHttpUrlWithoutCredentials(String value) {
        if (value == null) {
            return false;
        }

        try {
            URI uri = URI.create(value.trim());
            boolean httpScheme = "http".equalsIgnoreCase(uri.getScheme());
            boolean httpsScheme = "https".equalsIgnoreCase(uri.getScheme());

            return (httpScheme || httpsScheme)
                    && uri.getHost() != null
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
