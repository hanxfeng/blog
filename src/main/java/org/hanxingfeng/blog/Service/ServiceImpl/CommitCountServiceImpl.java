package org.hanxingfeng.blog.Service.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.CommitCount;
import org.hanxingfeng.blog.Mapper.CommitCountMapper;
import org.hanxingfeng.blog.Service.CommitCountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CommitCountServiceImpl extends ServiceImpl<CommitCountMapper, CommitCount> implements CommitCountService {

    @Autowired
    private CommitCountMapper commitCountMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> zsetRedisTemplate;

    // 只在当前类用，不影响其他地方的旧 ObjectMapper
    private final ObjectMapper list2NodeCountMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 获取昨日提交数据
     * @return 昨日的提交数据
     * @throws JsonProcessingException
     */
    @Override
    public CommitCount selectOneCommitCount() throws JsonProcessingException {
        // 先查 redis
        log.info("开始获取昨日提交数据");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String key = "CommitCount:" + yesterday;
        Object jsonData = redisTemplate.opsForValue().get(key);
        CommitCount re;
        // 判断是否有数据
        if (jsonData == null) {
            LambdaQueryWrapper<CommitCount> qw = new LambdaQueryWrapper<>();
            qw.eq(CommitCount::getCommitTime, yesterday);
            CommitCount cc = commitCountMapper.selectOne(qw);

            // 如果数据库也没有数据则抛出异常
            if (cc == null) {
                throw new IllegalArgumentException("数据异常");
            }
            re = cc;

            String redisData = objectMapper.writeValueAsString(re);
            redisTemplate.opsForValue().set(key, redisData, 36, TimeUnit.MINUTES);

        }
        else {
            re = objectMapper.readValue(jsonData.toString(), CommitCount.class);
        }

        return re;
    }

    /**
     * 获取所有修改代码行数
     * @return 今年以来所有的提交数据
     * @throws JsonProcessingException
     */
    @Cacheable(value = "commitCount", key = "'all'")
    @Override
    public List<CommitCount> selectAllCommit() throws JsonProcessingException {
        log.info("开始获取所有代码修改行数");
        String key = "CommitDataList:";
        byte[] rawKey = key.getBytes(StandardCharsets.UTF_8);
        Set<byte[]> members = zsetRedisTemplate.execute((RedisCallback<Set<byte[]>>) connection -> {
            return connection.zRange(rawKey, 0, 400);
        });
        List<CommitCount> re = new ArrayList<>();
        if (members != null && !members.isEmpty()) {
            try {
                for (byte[] member : members) {
                    re.add(objectMapper.readValue(member, CommitCount.class));
                }
                log.info("Redis 中获取历史代码数据，数量：{}", re.size());
                return re;
            } catch (Exception e) {
                log.warn("缓存数据损坏", e);
                return selectMySQL(rawKey);
            }
        }

        return selectMySQL(rawKey);

    }

    private List<CommitCount> selectMySQL (byte[] rawKey) {

        // 缓存不存在或已损坏，从 MySQL 中获取数据
        LocalDate startTime = Year.now().atDay(1);
        LocalDate endTime = LocalDate.now().minusDays(1);

        LambdaQueryWrapper<CommitCount> qw = new LambdaQueryWrapper<>();
        qw.between(CommitCount::getCommitTime, startTime, endTime);
        List<CommitCount> re2 = commitCountMapper.selectList(qw);


        // 使用 PipeLined 批量写入数据
        zsetRedisTemplate.executePipelined((RedisCallback<Object>) connection ->{
            for (CommitCount cc : re2) {
                try{
                    byte[] rawMember = objectMapper.writeValueAsBytes(cc);
                    long score = cc.getCommitTime().toEpochDay();
                    connection.zAdd(rawKey, score, rawMember);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            connection.expire(rawKey, 2400);
            return null;
        } );

        return re2;
    }

    /**
     * 用于获取总代码修改行数
     * @return 总代码修改行数
     */
    @Override
    public BigDecimal sumCommit() {
        log.info("开始获取总代码修改行数");

        String key = "sumCount";
        String redisData = redisTemplate.opsForValue().get(key);

        BigDecimal re;
        if (redisData == null) {
            QueryWrapper<CommitCount> qw = new QueryWrapper<>();
            qw.select("SUM(total_changes) as commit_count");
            Map<String, Object> map = commitCountMapper.selectMaps(qw).get(0);
            re = (BigDecimal) map.get("commit_count");
            redisTemplate.opsForValue().set(key, re.toString());
        }
        else {
            re = new BigDecimal(redisData);
            redisTemplate.opsForValue().set(key, String.valueOf(re));
        }

        return re;
    }
}
