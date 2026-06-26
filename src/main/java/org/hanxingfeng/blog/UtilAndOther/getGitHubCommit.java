package org.hanxingfeng.blog.UtilAndOther;

import org.hanxingfeng.blog.config.RepoConfig;
import org.hanxingfeng.blog.dto.GithubCommitDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class getGitHubCommit {
    @Autowired
    private WebClient githubWebClient;

    // 从 application.yml/properties 读取仓库所有者、仓库名和用户名
    @Value("${github.owner}")
    private String owner;

    @Value("${github.username}")
    private String username;

    private final RepoConfig repoConfig;

    public getGitHubCommit(RepoConfig repoConfig) {
        this.repoConfig = repoConfig;
    }

    public List<GithubCommitDTO> getYesterdayCommits(String repo) {
        // 查询昨日提交情况
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        String since = yesterday.atStartOfDay(ZoneOffset.UTC).toInstant().toString(); // 生成 2026-06-25T00:00:00Z
        String until = yesterday.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toString(); // 次日零点，避免 23:59:59 的毫秒误差
        return githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/commits")
                        .queryParam("author", username)
                        .queryParam("since", since)
                        .queryParam("until", until)
                        .build(owner, repo))   // 填充路径中的占位符
                .retrieve()                     // 获取响应体
                .bodyToFlux(GithubCommitDTO.class) // 将响应流解析为 Flux（异步流）
                .collectList()                  // 聚合成 List
                .block();                      // 阻塞等待结果返回（同步方式）
    }
}
