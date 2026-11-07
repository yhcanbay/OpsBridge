package com.yhcanbay.opsbridge.dto.request;

import java.net.URI;
import java.util.Set;

import com.yhcanbay.opsbridge.enums.Capability;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectRequest {

    @NotBlank
    @Size(max = 80)
    private String name;

    @NotBlank
    @Size(max = 500)
    private String repositoryUrl;

    @NotBlank
    @Pattern(regexp = "[a-z][a-z0-9_-]{0,29}")
    private String environment;

    @Size(max = 100)
    private String jenkinsBaseUrlRef;

    @Size(max = 200)
    private String jenkinsJobPath;

    @Size(max = 250)
    private String containerLabelSelector;

    @Size(max = 500)
    private String healthUrl;

    @NotNull
    private Set<@NotNull Capability> capabilities;

    @NotNull
    private Boolean enabled;

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
