package org.hanxingfeng.blog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.CommitCount;
import org.hanxingfeng.blog.Entity.GitCommit;
import org.hanxingfeng.blog.Entity.NodeCount;
import org.hanxingfeng.blog.Entity.User;
import org.hanxingfeng.blog.Mapper.BlogMapper;
import org.hanxingfeng.blog.Mapper.CommitCountMapper;
import org.hanxingfeng.blog.Mapper.CommitStatMapper;
import org.hanxingfeng.blog.Mapper.UserMapper;
import org.hanxingfeng.blog.Mapper.WritingsMapper;
import org.hanxingfeng.blog.UtilAndOther.UpdateNowData;
import org.hanxingfeng.blog.UtilAndOther.getGitHubCommit;
import org.hanxingfeng.blog.UtilAndOther.saveoneRepo;
import org.hanxingfeng.blog.config.RepoConfig;
import org.hanxingfeng.blog.dto.GithubCommitDTO;
import org.hanxingfeng.blog.dto.GithubCommitDetailDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.Key;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@SpringBootTest
class BlogApplicationTests {

    @Autowired
    private UpdateNowData updateNowData;

    @Autowired
    private WritingsMapper writingsMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WebClient githubWebClient;

    @Autowired
    private saveoneRepo saveoneRepo;

    // 从 application.yml/properties 读取仓库所有者、仓库名和用户名
    @Value("${github.owner}")
    private String owner;

    @Value("${github.username}")
    private String username;

    @Autowired
    private RepoConfig repoConfig;

    @Autowired
    private CommitStatMapper commitStatMapper;

    @Autowired
    private CommitCountMapper commitCountMapper;

    public void getRepo () {
        List<String> repos = repoConfig.getRepo();
    }

    @Autowired
    private getGitHubCommit getGitHubCommit;


    @Test
    void contextLoads() throws Exception {

        updateNowData.updateNoteData();
    }

    @Test
    void testWriting() throws JsonProcessingException {
        NodeCount nodeCount = new NodeCount();
        nodeCount.setTotalNoteCount(87);
        nodeCount.setTotalNoteWordCount(215929-600);
        nodeCount.setDailyNoteCount(0);
        nodeCount.setDailyNoteWordCount(7542);
        nodeCount.setTodayTime(LocalDate.now().minusDays(1));
        String jsonString = objectMapper.writeValueAsString(nodeCount);
        redisTemplate.opsForValue().set(LocalDate.now().minusDays(1).toString(), jsonString);
    }
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    @Test
    /**
     * 初始密码加密加密
     */
    void l() {
        User user = userMapper.selectById(1);
        String newPassword = passwordEncoder.encode("15263715582");
        user.setPassword(newPassword);
        userMapper.updateById(user);
    }

    @Test
    void loginTest(){
        User user = userMapper.selectById(1);
        String password = user.getPassword();
        if (passwordEncoder.matches("15263715582", password)) {
            System.out.println("成功：{}");
        }
    }


    @Test
    /**
     * 获取本年全部 commit 数据
     */
    void parse() {

        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(2);
        String since = yesterday.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
        String until = yesterday.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toString();
        List<GithubCommitDTO> r1e = githubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/commits")
                        .queryParam("author", username)
                        .queryParam("since", since)
                        .queryParam("until", until)
                        .build(owner, "blog"))
                .retrieve()
                .bodyToFlux(GithubCommitDTO.class)
                .collectList()
                .block();
        GithubCommitDetailDTO re = githubWebClient.get()
                .uri("/repos/{owner}/{repo}/commits/{sha}",
                        owner, "blog", "b73d4402a34909a768e60cbe015671efce4882ab")    // 按顺序填充三个占位符
                .retrieve()
                .bodyToMono(GithubCommitDetailDTO.class) // 解析为单个对象
                .block();                    // 同步阻塞



        System.out.println(re.toString());
    }


    @Test
    void saveCommitTest() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.now(ZoneOffset.UTC).minusDays(1);

        List<String> repos = repoConfig.getRepo();

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String since = date.atStartOfDay(ZoneOffset.UTC).toInstant().toString();
            String until = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toString();

            int dayAdditions = 0;
            int dayDeletions = 0;
            int dayTotal = 0;

            for (String repo : repos) {
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

                if (commits == null) {
                    continue;
                }

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
    }

    /**
     * 手动进行昨日数据更新
     */
    @Test
    void saveCommitTestSingleRepo() {
        getGitHubCommit.CommitStatService();
    }

}
