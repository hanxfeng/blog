package org.hanxingfeng.blog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

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
}
