package org.hanxingfeng.blog.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.*;
import org.hanxingfeng.blog.Mapper.BlogMapper;
import org.hanxingfeng.blog.Mapper.CommitCountMapper;
import org.hanxingfeng.blog.Mapper.SummaryWritingMapper;
import org.hanxingfeng.blog.Mapper.WritingsMapper;
import org.hanxingfeng.blog.UtilAndOther.UpdateNowData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private CommitCountMapper commitCountMapper;

    // 只在当前类用，不影响其他地方的旧 ObjectMapper
    private final ObjectMapper list2NodeCountMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 查询今日笔记数据
     * @return
     * @throws Exception
     */
    @GetMapping("/selectYesterday")
    public R<NodeCount> selectNow() throws Exception {
        log.info("开始执行 selectYesterday");
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
                log.info("Redis 中获取历史笔记数据");
                return R.success(re);
            } catch (Exception e) {
                log.info("缓存数据损坏");
            }

        } else {
            LambdaQueryWrapper<NodeCount> lqw = new LambdaQueryWrapper<>();
            lqw.ge(NodeCount::getTodayTime, START_TIME);
            List<NodeCount> re = blogMapper.selectList(lqw);
            log.info("MySQL 中获取历史笔记数据");
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
    public R<Page<SummaryWriting>> getBriefWritings (@RequestParam(defaultValue = "1") int current) throws JsonProcessingException {
        log.info("开始获取文章摘要");

        // 尝试从 Redis 中获取数据
        String key = "PageSummaryWriting:current=" + current + "size:9";
        String data = redisTemplate.opsForValue().get(key);

        if (data != null) {
            log.info("使用 Redis 数据获取文章摘要");
            Page resultPage = objectMapper.readValue(data, Page.class);
            return R.success(resultPage);
        }
        else {
            log.info("使用 MySQL 数据获取文章摘要");
            Page<SummaryWriting> page = new Page<>(current, 9);
            Page<SummaryWriting> resultPage = summaryWritingMapper.selectPage(page, null);

            // 添加缓存
            String redisData = objectMapper.writeValueAsString(resultPage);
            redisTemplate.opsForValue().set(key, redisData, 30, TimeUnit.MINUTES);
            return R.success(resultPage);
        }
    }

    /**
     * 获取指定文章
     */
    @GetMapping("/getOne/{id}")
    public R<Writings> getOne(@PathVariable Long id) throws JsonProcessingException {
        log.info("获取id为：{}的文章",id);
        String writing = redisTemplate.opsForValue().get("Writing" + id.toString());
        // 如果 Redis 中不存在数据，则从数据库中查询并写入 Redis
        Writings rWriting;
        if (writing == null) {
            rWriting = writingsMapper.selectById(id);
        }
        else {
            rWriting = objectMapper.readValue(writing, Writings.class);
        }
        rWriting.setViewCount(rWriting.getViewCount() + 1);
        writingsMapper.updateById(rWriting);
        String stringWriting = objectMapper.writeValueAsString(rWriting);
        redisTemplate.opsForValue().set("Writing" + id, stringWriting);

        return R.success(rWriting);
    }

    /**
     * 用于模糊查询，即搜索
     * @param keyLike
     * @return
     */
    @PostMapping("/selectWriting")
    public R<Page<SummaryWriting>> selectWriting(
            String keyLike,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<SummaryWriting> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SummaryWriting> qw = new LambdaQueryWrapper<>();
        qw.like(SummaryWriting::getTitle, keyLike);
        // 可选：添加排序
        qw.orderByDesc(SummaryWriting::getDate);

        Page<SummaryWriting> result = summaryWritingMapper.selectPage(page, qw);
        return R.success(result);
    }

    /**
     * 获取昨日提交数据
     * @return
     * @throws JsonProcessingException
     */
    @GetMapping("/selectOneCommitCount")
    public R<CommitCount> selectOneCommitCount() throws JsonProcessingException {
        // 先查 redis
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String key = "CommitCount:" + yesterday;
        Object jsonData = redisTemplate.opsForValue().get(key);
        CommitCount re;
        // 判断是否有数据
        if (jsonData == null) {
            LambdaQueryWrapper<CommitCount> qw = new LambdaQueryWrapper<>();
            qw.eq(CommitCount::getCommitTime, yesterday);
            CommitCount cc =commitCountMapper.selectOne(qw);

            // 如果数据库也没有数据则返回错误信息
            if (cc == null) {
                return R.error("数据异常");
            }
            re = cc;

            String redisData = objectMapper.writeValueAsString(re);
            redisTemplate.opsForValue().set(key, redisData);

        }
        else {
            re = objectMapper.readValue(jsonData.toString(), CommitCount.class);
        }

        return R.success(re);
    }

    /**
     * 获取所有修改代码行数
     * @return
     * @throws JsonProcessingException
     */
    @GetMapping("/selectAllCommit")
    public R<List<CommitCount>> selectAllCommit() throws JsonProcessingException {
        String key = "CommitDataList:";
        String redisData = redisTemplate.opsForValue().get(key);

        if (redisData != null) {
            try {
                List<CommitCount> re = list2NodeCountMapper.readValue(
                        redisData, new TypeReference<>() {
                        }
                );
                log.info("Redis 中获取历史笔记数据");
                return R.success(re);
            } catch (Exception e) {
                log.info("缓存数据损坏");
            }

        } else {
            LocalDate startTime = Year.now().atDay(1);
            LocalDate endTime = LocalDate.now().minusDays(1);

            LambdaQueryWrapper<CommitCount> qw = new LambdaQueryWrapper<>();
            qw.between(CommitCount::getCommitTime, startTime, endTime);
            List<CommitCount> re = commitCountMapper.selectList(qw);

            String json = list2NodeCountMapper.writeValueAsString(re);
            redisTemplate.opsForValue().set(key, json, 30, TimeUnit.MINUTES);
            return R.success(re);
        }

        return R.error("未知错误");


    }

    /**
     * 用于获取总代码修改行数
     * @return
     */
    @GetMapping("/sumCommit")
    public R<BigDecimal> sumCommit() {
        QueryWrapper<CommitCount> qw = new QueryWrapper<>();
        qw.select("SUM(total_changes) as commit_count");
        Map<String, Object> map = commitCountMapper.selectMaps(qw).get(0);
        return R.success((BigDecimal) map.get("commit_count"));
    }
}
