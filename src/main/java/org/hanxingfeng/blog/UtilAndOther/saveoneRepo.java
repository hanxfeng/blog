package org.hanxingfeng.blog.UtilAndOther;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.hanxingfeng.blog.Entity.CommitCount;
import org.hanxingfeng.blog.Entity.GitCommit;
import org.hanxingfeng.blog.Mapper.CommitCountMapper;
import org.hanxingfeng.blog.Mapper.CommitStatMapper;
import org.hanxingfeng.blog.dto.GithubCommitDTO;
import org.hanxingfeng.blog.dto.GithubCommitDetailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
@Component
public class saveoneRepo {

    @Autowired
    private WebClient githubWebClient;

    @Value("${github.owner}")
    private String owner;

    @Value("${github.username}")
    private String username;

    @Autowired
            private CommitStatMapper commitStatMapper;

    @Autowired
            private CommitCountMapper commitCountMapper;

    public void test() {
        String repo = "javaPractice";
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(1);

        // for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String since = date.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
            String until = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toString();

            int dayAdditions = 0;
            int dayDeletions = 0;
            int dayTotal = 0;

            List<GithubCommitDTO> commits = githubWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/commits")
                            .queryParam("author", username)
                            .queryParam("since", since)
                            .queryParam("until", until)
                            .build(owner, repo))
                    .retrieve()
                    .bodyToFlux(GithubCommitDTO.class)
                    .collectList()
                    .block();

            if (commits != null) {
                for (GithubCommitDTO commit : commits) {
                    String sha = commit.getSha();

                    LambdaQueryWrapper<GitCommit> qw = new LambdaQueryWrapper<>();
                    qw.eq(GitCommit::getCommitSha, sha);
                    if (commitStatMapper.selectOne(qw) != null) {
                        continue;
                    }

                    GithubCommitDetailDTO detail = githubWebClient.get()
                            .uri("/repos/{owner}/{repo}/commits/{sha}", owner, repo, sha)
                            .retrieve()
                            .bodyToMono(GithubCommitDetailDTO.class)
                            .block();

                    GitCommit gitCommit = new GitCommit();
                    gitCommit.setRepoName(repo);
                    gitCommit.setCommitSha(sha);
                    gitCommit.setCommitMessage(commit.getCommit().getMessage());
                    if (detail != null && detail.getStats() != null) {
                        gitCommit.setAddtions(detail.getStats().getAdditions());
                        gitCommit.setDeletions(detail.getStats().getDeletions());
                        gitCommit.setTotalChanges(detail.getStats().getTotal());
                        dayAdditions += detail.getStats().getAdditions() != null ? detail.getStats().getAdditions() : 0;
                        dayDeletions += detail.getStats().getDeletions() != null ? detail.getStats().getDeletions() : 0;
                        dayTotal += detail.getStats().getTotal() != null ? detail.getStats().getTotal() : 0;
                    }
                    gitCommit.setCommitTime(date);
                    gitCommit.setCreatedAt(LocalDateTime.now());
                    commitStatMapper.insert(gitCommit);
                }
            }

            LambdaQueryWrapper<CommitCount> countQw = new LambdaQueryWrapper<>();
            countQw.eq(CommitCount::getCommitTime, date);
            CommitCount existingCount = commitCountMapper.selectOne(countQw);
            if (existingCount != null) {
                existingCount.setAdditions(existingCount.getAdditions() + dayAdditions);
                existingCount.setDeletions(existingCount.getDeletions() + dayDeletions);
                existingCount.setTotalChanges(existingCount.getTotalChanges() + dayTotal);
                commitCountMapper.updateById(existingCount);
            } else {
                CommitCount commitCount = new CommitCount();
                commitCount.setAdditions(dayAdditions);
                commitCount.setDeletions(dayDeletions);
                commitCount.setTotalChanges(dayTotal);
                commitCount.setCommitTime(date);
                commitCountMapper.insert(commitCount);
        }
    }
}}
