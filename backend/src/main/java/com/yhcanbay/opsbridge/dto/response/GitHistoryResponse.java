package com.yhcanbay.opsbridge.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GitHistoryResponse {

    private List<String> branches;

    private int branchPage;

    private boolean hasMoreBranches;

    private String selectedBranch;

    private List<GitCommitResponse> commits;

    private int page;

    private boolean hasMoreCommits;

    private Instant fetchedAt;
}
