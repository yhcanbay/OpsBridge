package com.yhcanbay.opsbridge.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.yhcanbay.opsbridge.enums.IntegrationState;
import com.yhcanbay.opsbridge.exception.JenkinsException;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "opsbridge.jenkins")
@Getter
@Setter
public class JenkinsConfig {

    private String baseUrl = "";
    private String username = "";
    private String apiToken = "";
    private List<String> allowedBaseUrls = new ArrayList<>();

    @Bean
    public RestClient jenkinsRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(4));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    if (username.isBlank() != apiToken.isBlank()) {
                        throw new JenkinsException(IntegrationState.NOT_CONNECTED,
                                "JENKINS_USERNAME ve JENKINS_API_TOKEN birlikte tanımlanmalıdır.");
                    }

                    if (!username.isBlank()) {
                        // Yanlış yazılan bir proje URL'sine ortak Jenkins tokenını göndermeyiz.
                        if (!isTrustedAddress(request.getURI().toString())) {
                            throw new JenkinsException(IntegrationState.NOT_CONNECTED,
                                    "Bu Jenkins adresi için kimlik bilgisi gönderimine izin verilmedi. "
                                    + "Sunucudaki JENKINS_ALLOWED_BASE_URLS ayarını kontrol edin.");
                        }
                        try {
                            request.getHeaders().setBasicAuth(username, apiToken);
                        } catch (IllegalArgumentException exception) {
                            throw new JenkinsException(IntegrationState.NOT_CONNECTED,
                                    "Jenkins kullanıcı adı veya API tokenı geçerli Basic auth biçiminde değil.");
                        }
                    }

                    return execution.execute(request, body);
                })
                .build();
    }

    private boolean isTrustedAddress(String address) {
        List<String> trustedAddresses = new ArrayList<>(allowedBaseUrls);
        trustedAddresses.add(baseUrl);

        for (String trustedAddress : trustedAddresses) {
            String normalizedAddress = trustedAddress.trim().replaceAll("/+$", "");
            if (!normalizedAddress.isBlank() && address.startsWith(normalizedAddress + "/")) {
                return true;
            }
        }

        return false;
    }
}
