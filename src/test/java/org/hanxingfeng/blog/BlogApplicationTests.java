package org.hanxingfeng.blog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.NodeCount;
import org.hanxingfeng.blog.Entity.User;
import org.hanxingfeng.blog.Mapper.BlogMapper;
import org.hanxingfeng.blog.Mapper.UserMapper;
import org.hanxingfeng.blog.Mapper.WritingsMapper;
import org.hanxingfeng.blog.UtilAndOther.UpdateNowData;
import org.hanxingfeng.blog.UtilAndOther.getGitHubCommit;
import org.hanxingfeng.blog.config.RepoConfig;
import org.hanxingfeng.blog.dto.GithubCommitDTO;
import org.hanxingfeng.blog.dto.GithubCommitDetailDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.Key;
import java.time.LocalDate;
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

    // 从 application.yml/properties 读取仓库所有者、仓库名和用户名
    @Value("${github.owner}")
    private String owner;

    @Value("${github.username}")
    private String username;

    @Autowired
    private RepoConfig repoConfig;
    public void getRepo () {
        List<String> repos = repoConfig.getRepo();
    }


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
}
