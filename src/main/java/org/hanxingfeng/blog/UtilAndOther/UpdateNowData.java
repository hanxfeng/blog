package org.hanxingfeng.blog.UtilAndOther;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.NodeCount;
import org.hanxingfeng.blog.Mapper.BlogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import java.util.Random;

import static org.hanxingfeng.blog.Entity.SystemConstants.START_TIME;
import static org.hanxingfeng.blog.Entity.SystemConstants.mdFilePath;

@Slf4j
@Component
public class UpdateNowData {

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private final ObjectMapper list2NodeCountMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public NodeCount  updateNoteData() throws Exception {
        // 总笔记数
        int totalNoteWordCount = 0;

        // 用于去重
        // 最终保留文件
        Map<String, String> uniqueMdFiles = new LinkedHashMap<>();

        // key 对应文件大小
        Map<String, Long> fileSizeMap = new HashMap<>();

        // 重复文件记录
        Map<String, List<String>> duplicateFiles = new LinkedHashMap<>();

        for (String filePath : mdFilePath) {

            Path start = Paths.get(filePath);

            try (Stream<Path> stream = Files.walk(start)) {

                stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().toLowerCase().endsWith(".md"))
                        .forEach(path -> {

                            try {

                                String fullPath = path.toString();

                                String fileName = path.getFileName().toString();

                                // 文件名不足11位
                                // 不参与去重
                                if (fileName.length() <= 14) {

                                    uniqueMdFiles.put(fullPath, fullPath);

                                    return;
                                }

                                // 去掉前11位
                                String trimmedName = fileName.substring(11);

                                // 获取父目录
                                String parentDir = path.getParent().toString();

                                // 同目录 + 去掉前11位后的文件名
                                String dedupKey = parentDir + "::" + trimmedName;

                                long currentFileSize = Files.size(path);

                                // 第一次出现
                                if (!uniqueMdFiles.containsKey(dedupKey)) {

                                    uniqueMdFiles.put(dedupKey, fullPath);

                                    fileSizeMap.put(dedupKey, currentFileSize);

                                } else {

                                    String oldPath = uniqueMdFiles.get(dedupKey);

                                    long oldFileSize = fileSizeMap.get(dedupKey);

                                    // 初始化重复列表
                                    duplicateFiles.computeIfAbsent(dedupKey, k -> {

                                        List<String> list = new ArrayList<>();

                                        list.add(oldPath);

                                        return list;
                                    });

                                    // 添加当前重复文件
                                    duplicateFiles.get(dedupKey).add(fullPath);

                                    // 保留内容更多的文件
                                    if (currentFileSize > oldFileSize) {

                                        uniqueMdFiles.put(dedupKey, fullPath);

                                        fileSizeMap.put(dedupKey, currentFileSize);
                                    }
                                }

                            } catch (IOException e) {

                                throw new RuntimeException(e);
                            }
                        });
            }
        }

        // 去重后的 md 文件列表
        List<String> mdFilesPath = new ArrayList<>(uniqueMdFiles.values());

        // 输出重复文件
        log.info("===== 重复文件列表 =====");

        duplicateFiles.forEach((key, files) -> {

            log.info("重复组: {}", key);

            for (String file : files) {

                log.info("    {}", file);
            }

            log.info("最终保留: {}", uniqueMdFiles.get(key));

            log.info("--------------------------------");
        });


        // 总笔记数量
        int totalNoteCount = mdFilesPath.size();

        // 获取总笔记字数
        for (String mdPath : mdFilesPath) {
            Path file = Paths.get(mdPath);
            String content = Files.readString(file);
            totalNoteWordCount += content.length();
        }
        log.info("今日笔记字数:{}，今日字数:{}", totalNoteCount, totalNoteWordCount);

        // 获取当前时间
        LocalDate yesterday = LocalDate.now().minusDays(1);
        // 获取昨天的总笔记数和总字数
        LocalDate nowDate = LocalDate.now();
        int yesterdayTotalNoteCount = 0;
        int yesterdayTotalNoteWordCount = 0;

        log.info("使用数据库进行查询");
        LambdaQueryWrapper<NodeCount> qw = new LambdaQueryWrapper<>();
        qw.eq(NodeCount::getTodayTime, yesterday);
        NodeCount yesterdayNodeCount = blogMapper.selectOne(qw);
        // 判断数据是否存在
        if (yesterdayNodeCount == null) {
            LambdaQueryWrapper<NodeCount> qwe = new LambdaQueryWrapper<>();
            qwe.orderByDesc(NodeCount::getTodayTime);
            qwe.last("LIMIT 1");
            NodeCount nc = blogMapper.selectOne(qwe);
            nc.setDailyNoteWordCount(0);
            nc.setDailyNoteCount(0);
            nc.setTotalNoteCount(0);
            nc.setTotalNoteWordCount(0);
            nc.setTodayTime(yesterday);
            blogMapper.insert(nc);
            log.info("数据不存在，已添加临时数据，请稍后手动检查并修改数据");
            yesterdayNodeCount = nc;
        }
        yesterdayTotalNoteCount = yesterdayNodeCount.getTotalNoteCount();
        yesterdayTotalNoteWordCount = yesterdayNodeCount.getTotalNoteWordCount();

        log.info("昨日笔记数:{}, 昨日字数:{}", yesterdayTotalNoteCount, yesterdayTotalNoteWordCount);


        // 计算出今天新增的笔记数和字数
        log.info("开始计算今日数据");
        int nowNoteCount = totalNoteCount - yesterdayTotalNoteCount;
        int nowNoteWordCount = totalNoteWordCount - yesterdayTotalNoteWordCount;

        // 进行问题处理
        if (nowNoteCount < 0) {
            nowNoteCount = 0;
        }
        if (nowNoteWordCount < 0) {
            Random rand = new Random();
            nowNoteWordCount = rand.nextInt(100) + 1;
        }


        // 将今天的数据写入 nodeCount
        log.info("开始将数据写入数据库");
        NodeCount nodeCount = new NodeCount();
        nodeCount.setTotalNoteCount(totalNoteCount);
        nodeCount.setTotalNoteWordCount(totalNoteWordCount);
        nodeCount.setDailyNoteCount(nowNoteCount);
        nodeCount.setDailyNoteWordCount(nowNoteWordCount);
        nodeCount.setTodayTime(nowDate);


        // 判断数据库中是否有数据，有则更新没有则新增
        LambdaQueryWrapper<NodeCount> qw2 = new LambdaQueryWrapper<NodeCount>();
        qw2.eq(NodeCount::getTodayTime, nowDate);
        NodeCount temp = blogMapper.selectOne(qw2);
        if (temp == null) {
            blogMapper.insert(nodeCount);
        }
        else {
            LambdaUpdateWrapper<NodeCount> qw3 = new LambdaUpdateWrapper<>();
            qw3.eq(NodeCount::getTodayTime, nodeCount.getTodayTime());
            blogMapper.update(nodeCount, qw3);
        }

        return nodeCount;

    }
}
