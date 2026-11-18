package com.yhcanbay.opsbridge.exception;

import com.yhcanbay.opsbridge.enums.IntegrationState;

import lombok.Getter;

@Getter
public class JenkinsException extends RuntimeException {

    private final IntegrationState state;

    public JenkinsException(IntegrationState state, String message) {
        super(message);
        this.state = state;
    }
}
