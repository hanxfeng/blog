package org.hanxingfeng.blog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.NodeCount;
import org.hanxingfeng.blog.Entity.Writings;
import org.hanxingfeng.blog.Mapper.BlogMapper;
import org.hanxingfeng.blog.Mapper.WritingsMapper;
import org.hanxingfeng.blog.other.UpdateNowData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.Key;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import static io.jsonwebtoken.security.Keys.secretKeyFor;
import static org.hanxingfeng.blog.Entity.SystemConstants.UPLOAD_DIR;

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
     * JWT 加密
     */
    void jwt() {
        System.out.println(SECRET_KEY.toString());
        JwtBuilder jwtBuilder = Jwts.builder();
        String toke = jwtBuilder
                // 添加 Header
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("alg", "HS256")
                // 添加 Payload
                .claim("userName", "hxf") // 私有声明
                .setSubject("2654826") // 注册声明
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 一小时后过期
                // 添加 Signature
                .signWith(SECRET_KEY)
                // 将以上三部分用 . 拼接
                .compact();
        System.out.println(toke);

        Jws<Claims> claimsJwt = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)          // SECRET_KEY 可为 String/byte[]/Key
                .build()                            // 构建 JwtParser
                .parseClaimsJws(toke);             // 解析并验证 JWS

        Claims claims = claimsJwt.getBody();
        System.out.println(claims.get("userName"));
    }

    @Test
    void parse() {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyTmFtZSI6Imh4ZiIsInN1YiI6IjI2NTQ4MjYiLCJleHAiOjE3ODIwMzEyMDB9.HqNtO2zVPJ74hKL2TbUaYV7-eeJ_X1p6Nq8xBzHKTEo";

        // 使用 parserBuilder() 替代废弃的 parser()
        Jws<Claims> claimsJwt = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)          // SECRET_KEY 可为 String/byte[]/Key
                .build()                            // 构建 JwtParser
                .parseClaimsJws(token);             // 解析并验证 JWS

        Claims claims = claimsJwt.getBody();
        System.out.println(claims.get("userName"));
    }
}
