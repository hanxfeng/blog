package org.hanxingfeng.blog.UtilAndOther;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.GitCommit;
import org.hanxingfeng.blog.Mapper.CommitStatMapper;
import org.hanxingfeng.blog.config.RepoConfig;
import org.hanxingfeng.blog.dto.GithubCommitDTO;
import org.hanxingfeng.blog.dto.GithubCommitDetailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
@Slf4j
public class getGitHubCommit {
    @Autowired
    private WebClient githubWebClient;

    // 从 application.yml/properties 读取仓库所有者、仓库名和用户名
    @Value("${github.owner}")
    private String owner;

    @Value("${github.username}")
    private String username;

    @Autowired
    private RepoConfig repoConfig;

    @Autowired
    private CommitStatMapper commitStatMapper;


    /**
     * 获取当前用户今日在指定仓库中的所有提交记录
     * @param repo:指定仓库名
     * @return 今日提交的 DTO 列表，每个包含 sha、commit.message、commit.author.date 等信息。
     */
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


    /**
     * 根据提交 SHA 获取该次提交的详细信息
     * @param sha：提交的唯一哈希值
     * @param repo：指定的仓库
     * @return 提交详情 DTO，内含 additions, deletions, total 等信息。
     */
    public GithubCommitDetailDTO getCommitDetail(String sha, String repo) {
        // 直接使用占位符填充路径，返回 Mono（异步单个结果）后阻塞获取
        return githubWebClient.get()
                .uri("/repos/{owner}/{repo}/commits/{sha}",
                        owner, repo, sha)    // 按顺序填充三个占位符
                .retrieve()
                .bodyToMono(GithubCommitDetailDTO.class) // 解析为单个对象
                .block();                    // 同步阻塞
    }

    /**
     * 获取并保存今日数据
     */
    @Retryable(maxAttempts = 144, backoff = @Backoff(delay = 600000))
    public void CommitStatService() {
        List<String> repos = repoConfig.getRepo();
        for (String repo : repos) {
            List<GithubCommitDTO> commits = getYesterdayCommits(repo);

            for (GithubCommitDTO commit : commits) {
                String sha = commit.getSha();

                LambdaQueryWrapper<GitCommit> qw = new LambdaQueryWrapper<>();
                qw.eq(GitCommit::getCommitSha, sha);
                GitCommit re = commitStatMapper.selectOne(qw);
                if (re != null) {
                    continue;
                }

                GithubCommitDetailDTO detail = getCommitDetail(sha, repo);

                GitCommit gitCommit = new GitCommit();
                gitCommit.setRepoName(repo);
                gitCommit.setCommitSha(sha);
                gitCommit.setCommitMessage(commit.getCommit().getMessage());
                gitCommit.setAddtions(detail.getStats().getAdditions());
                gitCommit.setDeletions(detail.getStats().getDeletions());
                gitCommit.setTotalChanges(detail.getStats().getTotal());
                String dateStr = commit.getCommit().getAuthor().getDate().replace("Z", "");
                gitCommit.setCommitTime(LocalDate.parse(dateStr));
                gitCommit.setCreatedAt(LocalDateTime.now());

                commitStatMapper.insert(gitCommit);
            }
        }
    }
}
