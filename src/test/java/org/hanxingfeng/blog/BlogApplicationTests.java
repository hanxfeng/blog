package org.hanxingfeng.blog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.CommitCount;
import org.hanxingfeng.blog.Entity.GitCommit;
import org.hanxingfeng.blog.Entity.Node;
import org.hanxingfeng.blog.Entity.NodeCount;
import org.hanxingfeng.blog.Entity.SystemConstants;
import org.hanxingfeng.blog.Entity.User;
import org.hanxingfeng.blog.Mapper.BlogMapper;
import org.hanxingfeng.blog.Mapper.CommitCountMapper;
import org.hanxingfeng.blog.Mapper.CommitStatMapper;
import org.hanxingfeng.blog.Mapper.NodeMapper;
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

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    @Autowired
    private NodeMapper nodeMapper;

    public void getRepo () {
        List<String> repos = repoConfig.getRepo();
    }

    @Autowired
    private getGitHubCommit getGitHubCommit;


    @Test
    void testData() {
        LocalDate startTime = Year.now().atDay(1);
        LocalDate endTime = LocalDate.now().minusDays(1);

        LambdaQueryWrapper<CommitCount> qw = new LambdaQueryWrapper<>();
        qw.between(CommitCount::getCommitTime, startTime, endTime);
        List<CommitCount> re = commitCountMapper.selectList(qw);
        System.out.println(re);
    }

    // 用于手动更新数据
    @Test
    void contextLoads() throws Exception {

        updateNowData.updateNoteData();
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

    // ==================== 数据恢复 ====================
    // 数据库被清空后，按需依次执行以下方法即可恢复：
    //   1. recoverAdminUser()      恢复管理员账号（user 表）
    //   2. recoverNotes()          恢复笔记正文（node 表）
    //   3. recoverNoteStatistics() 恢复笔记统计（node_count 表）
    //   4. saveCommitTest()        恢复 GitHub 提交历史（git_commit / commit_count 表）
    //   （writings / summary_writing 文章数据无法自动恢复，请通过管理后台重新上传）
    //   （Redis 为缓存，会在访问时自动重建，无需手动恢复）

    /**
     * 恢复管理员账号（user 表）
     */
    @Test
    void recoverAdminUser() {
        String userName = "hanxfeng";
        String rawPassword = "15263715582";

        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUserName, userName);
        User user = userMapper.selectOne(qw);

        if (user == null) {
            user = new User();
            user.setUserName(userName);
            user.setRole(1);
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (user.getUserId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        log.info("管理员账号已恢复：{}", userName);
    }

    /**
     * 恢复笔记正文（node 表），从本地 md 目录扫描
     */
    @Test
    void recoverNotes() throws Exception {
        List<String> files = scanMdFiles();
        int inserted = 0;
        for (String fullPath : files) {
            String title = toRelativeTitle(fullPath);
            LambdaQueryWrapper<Node> qw = new LambdaQueryWrapper<>();
            qw.eq(Node::getTitle, title);
            if (nodeMapper.selectOne(qw) != null) {
                continue;
            }
            Node node = new Node();
            node.setTitle(title);
            node.setBody(Files.readString(Paths.get(fullPath)));
            nodeMapper.insert(node);
            inserted++;
        }
        log.info("笔记正文恢复完成，共 {} 篇", inserted);
    }

    /**
     * 恢复笔记统计（node_count 表）
     * 需求变更：不再统计「每日更新字数」，仅统计总字数，故每日字数置 0
     */
    @Test
    void recoverNoteStatistics() throws Exception {
        List<String> files = scanMdFiles();
        int totalNoteCount = files.size();
        int totalNoteWordCount = 0;
        for (String fullPath : files) {
            totalNoteWordCount += Files.readString(Paths.get(fullPath)).length();
        }

        NodeCount nodeCount = new NodeCount();
        nodeCount.setDailyNoteWordCount(0);
        nodeCount.setDailyNoteCount(0);
        nodeCount.setTotalNoteCount(totalNoteCount);
        nodeCount.setTotalNoteWordCount(totalNoteWordCount);
        nodeCount.setTodayTime(LocalDate.now());

        LambdaQueryWrapper<NodeCount> qw = new LambdaQueryWrapper<>();
        qw.eq(NodeCount::getTodayTime, LocalDate.now());
        if (blogMapper.selectOne(qw) == null) {
            blogMapper.insert(nodeCount);
        } else {
            blogMapper.updateById(nodeCount);
        }
        log.info("笔记统计恢复完成：总数 {}，总字数 {}", totalNoteCount, totalNoteWordCount);
    }

    /**
     * 扫描本地 md 目录并按去重规则返回最终保留的文件绝对路径（与 UpdateNowData 去重规则一致）
     */
    private List<String> scanMdFiles() throws Exception {
        Map<String, String> uniqueMdFiles = new LinkedHashMap<>();
        Map<String, Long> fileSizeMap = new HashMap<>();
        for (String filePath : SystemConstants.mdFilePath) {
            Path start = Paths.get(filePath);
            try (Stream<Path> stream = Files.walk(start)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.toString().toLowerCase().endsWith(".md"))
                        .forEach(path -> {
                            try {
                                String fullPath = path.toString();
                                String fileName = path.getFileName().toString();
                                if (fileName.length() <= 14) {
                                    uniqueMdFiles.put(fullPath, fullPath);
                                    return;
                                }
                                String trimmedName = fileName.substring(11);
                                String parentDir = path.getParent().toString();
                                String dedupKey = parentDir + "::" + trimmedName;
                                long currentFileSize = Files.size(path);
                                if (!uniqueMdFiles.containsKey(dedupKey)) {
                                    uniqueMdFiles.put(dedupKey, fullPath);
                                    fileSizeMap.put(dedupKey, currentFileSize);
                                } else if (currentFileSize > fileSizeMap.get(dedupKey)) {
                                    uniqueMdFiles.put(dedupKey, fullPath);
                                    fileSizeMap.put(dedupKey, currentFileSize);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }
        return new ArrayList<>(uniqueMdFiles.values());
    }

    /**
     * 计算 md 文件相对于笔记根目录的相对路径（作为 node 表的 title）
     */
    private String toRelativeTitle(String fullPath) {
        Path abs = Paths.get(fullPath).toAbsolutePath();
        for (String root : SystemConstants.mdFilePath) {
            Path rootPath = Paths.get(root).toAbsolutePath();
            if (abs.startsWith(rootPath)) {
                return rootPath.relativize(abs).toString().replace('\\', '/');
            }
        }
        return Paths.get(fullPath).getFileName().toString();
    }

}
