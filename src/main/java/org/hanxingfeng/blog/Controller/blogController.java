package org.hanxingfeng.blog.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.*;
import org.hanxingfeng.blog.Service.BlogService;
import org.hanxingfeng.blog.Service.CommitCountService;
import org.hanxingfeng.blog.Service.SummaryWritingService;
import org.hanxingfeng.blog.Service.WritingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/blog")
@Slf4j
public class blogController {
    // TODO：增加防止缓存击穿，穿透，雪崩的措施

    @Autowired
    private BlogService blogService;

    @Autowired
    private SummaryWritingService summaryWritingService;

    @Autowired
    private WritingsService writingsService;

    @Autowired
    private CommitCountService commitCountService;

    /** // 0.024 - 0.233
     * 查询今日笔记数据
     * @return
     * @throws Exception
     */
    @GetMapping("/selectYesterday")
    public R<NodeCount> selectNow() throws Exception {
        return R.success(blogService.selectYesterday());
    }

    /** // 0.024 - 0.234
     * 查询历史笔记数据
     * @return
     */
    @GetMapping("/select")
    public R<List<NodeCount>> select() throws JsonProcessingException {
        return R.success(blogService.select());
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

    /** // 0.24 - 0.687
     * 获取所有文章摘要
     */
    @GetMapping("/getBriefWritings")
    public R<Page<SummaryWriting>> getBriefWritings (@RequestParam(defaultValue = "1") int current) throws JsonProcessingException {
        return R.success(summaryWritingService.getBriefWritings(current));
    }

    /** // 0.246 - 4.384
     * 获取指定文章
     */
    @GetMapping("/getOne/{id}")
    public R<Writings> getOne(@PathVariable Long id) throws JsonProcessingException {
        if (id < 0) {
            return R.error("id 错误");
        }
        return R.success(writingsService.getWritingsById(id));
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

        return R.success(summaryWritingService.selectWriting(keyLike, pageNum, pageSize));
    }

    /** // 0.024 - 0.067
     * 获取昨日提交数据
     * @return
     * @throws JsonProcessingException
     */
    @GetMapping("/selectOneCommitCount")
    public R<CommitCount> selectOneCommitCount() throws JsonProcessingException {
        try {
            return R.success(commitCountService.selectOneCommitCount());
        }
        catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        }
    }

    /** 0.033 - 12.454 降至 最大0.003
     * 获取所有修改代码行数
     * @return
     * @throws JsonProcessingException
     */
    @GetMapping("/selectAllCommit")
    public R<List<CommitCount>> selectAllCommit() throws JsonProcessingException {
        return R.success(commitCountService.selectAllCommit());
    }

    /** // 0.024 - 0.029
     * 用于获取总代码修改行数
     * @return
     */
    @GetMapping("/sumCommit")
    public R<Map> sumCommit() throws JsonProcessingException {
        return R.success(commitCountService.sumCommit());
    }
}
