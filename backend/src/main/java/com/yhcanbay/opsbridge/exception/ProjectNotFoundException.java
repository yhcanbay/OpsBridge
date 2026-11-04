package com.yhcanbay.opsbridge.exception;

import java.util.UUID;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(UUID projectId) {
        super("Proje bulunamadı: " + projectId);
    }
}
