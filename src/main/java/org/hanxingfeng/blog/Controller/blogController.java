package org.hanxingfeng.blog.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ibm.icu.impl.ICULocaleService;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.NodeCount;
import org.hanxingfeng.blog.Entity.R;
import org.hanxingfeng.blog.Entity.SummaryWriting;
import org.hanxingfeng.blog.Entity.Writings;
import org.hanxingfeng.blog.Mapper.BlogMapper;
import org.hanxingfeng.blog.Mapper.SummaryWritingMapper;
import org.hanxingfeng.blog.Mapper.WritingsMapper;
import org.hanxingfeng.blog.other.UpdateNowData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;


import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.hanxingfeng.blog.Entity.SystemConstants.START_TIME;

@RestController
@RequestMapping("/blog")
@Slf4j
public class blogController {

    @Autowired
    private UpdateNowData updateNowData;

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SummaryWritingMapper summaryWritingMapper;

    @Autowired
    private WritingsMapper writingsMapper;

    // 只在当前类用，不影响其他地方的旧 ObjectMapper
    private final ObjectMapper list2NodeCountMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 查询今日笔记数据
     * @return
     * @throws Exception
     */
    @GetMapping("/selectNow")
    public R<NodeCount> selectNow() throws Exception {
        log.info("开始执行 selectNow");
        NodeCount nodeCount = new NodeCount();
        // 1. 从redis中获取数据
        LocalDate yesterdayDate = LocalDate.now().minusDays(1);
        Object jsonData = redisTemplate.opsForValue().get(yesterdayDate.toString());

        // 判断是否有数据
        if (jsonData == null) {
            // 没有则尝试从数据库中获取数据
            LambdaQueryWrapper<NodeCount> qw = new LambdaQueryWrapper<>();
            qw.eq(NodeCount::getTodayTime, yesterdayDate);
            NodeCount nowData = blogMapper.selectOne(qw);

            // 如果数据库也没有数据则调用 updateNowData 方法
            if (nowData == null) {
                nowData = updateNowData.updateNoteData();
            }
            nodeCount = nowData;
        }
        else {
            nodeCount = objectMapper.readValue(jsonData.toString(), NodeCount.class);
        }

        return R.success(nodeCount);
    }

    /**
     * 查询历史笔记数据
     * @return
     */
    @GetMapping("/select")
    public R<List<NodeCount>> select() {
        log.info("开始执行 select");

        // 先尝试在 redis 中获取数据
        String CACHE_KEY = "nodeCountList";
        String redisData = redisTemplate.opsForValue().get(CACHE_KEY);

        if (redisData != null) {
            try {
                List<NodeCount> re = list2NodeCountMapper.readValue(
                        redisData, new TypeReference<>() {
                        }
                );

                return R.success(re);
            } catch (Exception e) {
                log.info("缓存数据损坏");
            }

        } else {
            LambdaQueryWrapper<NodeCount> lqw = new LambdaQueryWrapper<>();
            lqw.ge(NodeCount::getTodayTime, START_TIME);
            List<NodeCount> re = blogMapper.selectList(lqw);
            return R.success(re);
        }

        return R.error("未知错误");
    }

    /**
     * 测试方法，用于测试服务器运行状况
     * @return
     */
    @GetMapping("/test")
    public R<String> testS () {
        log.info("服务器正常运行");
        return R.success("服务器正常运行");
    }

    /**
     * 获取所有文章摘要
     */
    @GetMapping("/getBriefWritings")
    public R<Page<SummaryWriting>> getBriefWritings (@RequestParam(defaultValue = "1") int current) {
        log.info("开始获取文章摘要");
        Page<SummaryWriting> page = new Page<>(current, 9);
        Page<SummaryWriting> resultPage = summaryWritingMapper.selectPage(page, null);
        return R.success(resultPage);
    }

    /**
     * 获取指定文章
     */
    @GetMapping("/getOne/{id}")
    public R<Writings> getOne(@PathVariable Long id) {
        log.info("获取id为：{}的文章",id);
        return R.success(writingsMapper.selectById(id));
    }
}
