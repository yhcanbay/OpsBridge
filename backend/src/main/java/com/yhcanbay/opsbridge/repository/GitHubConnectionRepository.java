package com.yhcanbay.opsbridge.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yhcanbay.opsbridge.entity.GitHubConnection;

public interface GitHubConnectionRepository extends JpaRepository<GitHubConnection, UUID> {

    List<GitHubConnection> findAllByOrderByAccountLoginAscNameAsc();
}
