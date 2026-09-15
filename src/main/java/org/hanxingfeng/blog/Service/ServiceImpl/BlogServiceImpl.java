package org.hanxingfeng.blog.Service.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.NodeCount;
import org.hanxingfeng.blog.Mapper.BlogMapper;
import org.hanxingfeng.blog.Service.BlogService;
import org.hanxingfeng.blog.UtilAndOther.UpdateNowData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hanxingfeng.blog.Entity.SystemConstants.START_TIME;

@Service
@Slf4j

public class BlogServiceImpl extends ServiceImpl<BlogMapper, NodeCount> implements BlogService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private UpdateNowData updateNowData;

    @Autowired
    private ObjectMapper objectMapper;

    // 自定义 ObjectMapper
    private final ObjectMapper list2NodeCountMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 查询今日笔记数据
     * @return
     * @throws Exception
     */
    @Override
    public NodeCount selectYesterday() throws Exception {
        log.info("开始执行 selectYesterday");
        NodeCount nodeCount;
        // 1. 从redis中获取数据
        LocalDate key = LocalDate.now().minusDays(1);
        Object jsonData = redisTemplate.opsForValue().get(key.toString());

        // 判断是否有数据
        if (jsonData == null) {
            // 没有则尝试从数据库中获取数据
            LambdaQueryWrapper<NodeCount> qw = new LambdaQueryWrapper<>();
            qw.eq(NodeCount::getTodayTime, key);
            NodeCount yesterdayData = blogMapper.selectOne(qw);

            // 如果数据库也没有数据则调用 updateNowData 方法
            if (yesterdayData == null) {
                yesterdayData = updateNowData.updateNoteData();
            }

            // 将数据保存至 Redis
            String redisData = objectMapper.writeValueAsString(yesterdayData);
            redisTemplate.opsForValue().set(key.toString(), redisData, 30, TimeUnit.MINUTES);

            nodeCount = yesterdayData;
        }
        else {
            nodeCount = objectMapper.readValue(jsonData.toString(), NodeCount.class);
        }
        return nodeCount;
    }

    /**
     * 查询历史笔记数据
     * @return
     * @throws JsonProcessingException
     */
    @Override
    public List<NodeCount> select() throws JsonProcessingException {
        log.info("开始执行 select");

        // 先尝试在 redis 中获取数据
        String key = "nodeCountList";
        String nodeCountList = redisTemplate.opsForValue().get(key);

        if (nodeCountList != null) {
            try {
                List<NodeCount> re = list2NodeCountMapper.readValue(
                        nodeCountList, new TypeReference<>() {
                        }
                );
                log.info("Redis 中获取历史笔记数据");
                return re;
            } catch (Exception e) {
                log.info("缓存数据损坏");
            }

        }

        // 缓存不存在或已损坏，从 MySQL 中获取数据
        LambdaQueryWrapper<NodeCount> lqw = new LambdaQueryWrapper<>();
        lqw.ge(NodeCount::getTodayTime, START_TIME);
        List<NodeCount> re = blogMapper.selectList(lqw);
        log.info("MySQL 中获取历史笔记数据");

        // 将数据写入 Redis
        String redisData = list2NodeCountMapper.writeValueAsString(re);
        redisTemplate.opsForValue().set(key, redisData, 30, TimeUnit.MINUTES);

        return re;
    }
}
