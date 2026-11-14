package com.yhcanbay.opsbridge.exception;

public class GitHubConnectionNotFoundException extends RuntimeException {

    public GitHubConnectionNotFoundException() {
        super("GitHub bağlantısı bulunamadı. Bağlantı listesini yenileyin.");
    }
}
