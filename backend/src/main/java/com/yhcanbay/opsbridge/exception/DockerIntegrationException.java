package com.yhcanbay.opsbridge.exception;

import com.yhcanbay.opsbridge.enums.IntegrationState;

import lombok.Getter;

@Getter
public class DockerIntegrationException extends RuntimeException {

    private final IntegrationState state;

    public DockerIntegrationException(IntegrationState state, String message) {
        super(message);
        this.state = state;
    }
}
