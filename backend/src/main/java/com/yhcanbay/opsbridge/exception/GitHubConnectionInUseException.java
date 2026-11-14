package com.yhcanbay.opsbridge.exception;

public class GitHubConnectionInUseException extends RuntimeException {

    public GitHubConnectionInUseException() {
        super("Bu GitHub bağlantısı projelerde kullanılıyor. Silmeden önce projelerin bağlantısını değiştirin.");
    }
}
