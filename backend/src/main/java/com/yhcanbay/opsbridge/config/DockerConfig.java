package com.yhcanbay.opsbridge.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

@Configuration
public class DockerConfig {

    @Bean
    public DockerClient dockerClient(@Value("${opsbridge.docker.host}") String dockerHost) {
        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        if (!dockerHost.isBlank()) {
            builder.withDockerHost(dockerHost.trim());
        }
        DockerClientConfig config = builder.build();
        URI host = config.getDockerHost();
        // Unix socket ve Windows named pipe, aynı makinedeki süreçlerin haberleşmesini sağlar.
        boolean unixSocket = "unix".equals(host.getScheme()) && host.getAuthority() == null;
        boolean windowsPipe = "npipe".equals(host.getScheme()) && host.getPath() != null
                && host.getPath().startsWith("//./pipe/");
        if (!unixSocket && !windowsPipe) {
            throw new IllegalStateException("OpsBridge Docker bağlantısı için yerel Unix socket veya Windows named pipe kullanın.");
        }

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(host)
                .connectionTimeout(Duration.ofSeconds(2))
                .responseTimeout(Duration.ofSeconds(3))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
}
