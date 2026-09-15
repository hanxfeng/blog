package org.hanxingfeng.blog.Service.ServiceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.SummaryWriting;
import org.hanxingfeng.blog.Entity.Writings;
import org.hanxingfeng.blog.Mapper.SummaryWritingMapper;
import org.hanxingfeng.blog.Mapper.WritingsMapper;
import org.hanxingfeng.blog.Service.SummaryWritingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SummaryWritingServiceImpl extends ServiceImpl<SummaryWritingMapper, SummaryWriting> implements SummaryWritingService {

    @Autowired
    private WritingsMapper writingsMapper;

    @Autowired
    private SummaryWritingMapper summaryWritingMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public void saveWriting(Writings writings, SummaryWriting summaryWriting) {
        writingsMapper.insert(writings);
        summaryWritingMapper.insert(summaryWriting);
        log.info("文件保存成功！");

    }

    @Transactional
    @Override
    public void deleteWriting(Long id) {
        writingsMapper.deleteById(id);
        summaryWritingMapper.deleteById(id);

        // 在 Redis 中删除
        redisTemplate.delete("Writing" + id.toString());
    }

    /**
     * 上传 markdown 文章
     * @param fileName 原始文件名（含 .md 后缀）
     * @param content 文章内容
     * @throws JsonProcessingException
     */
    @Override
    public void uploadWriting(String fileName, String content) throws JsonProcessingException {
        // 判断是否有同样的文章
        fileName = fileName.substring(0, fileName.length() - 3);
        LambdaQueryWrapper<Writings> qw = new LambdaQueryWrapper<>();
        qw.eq(Writings::getTitle, fileName);
        Writings writingsTemp = writingsMapper.selectOne(qw);
        if (writingsTemp != null) {
            throw new IllegalArgumentException("已上传过同名文章");
        }

        int endIndex = Math.min(content.length(), 20);

        String preview = content.substring(0, endIndex);

        // 返回 Writings 类
        log.info("上传文件名：{}",fileName);
        Writings writings = new Writings();
        writings.setTitle(fileName);
        writings.setContent(content);
        writings.setPreview(preview);
        writings.setDate(LocalDate.now());

        SummaryWriting summaryWriting = new SummaryWriting();
        summaryWriting.setTitle(fileName);
        summaryWriting.setPreview(preview);
        summaryWriting.setDate(LocalDate.now());

        // 将数据保存至数据库
        saveWriting(writings, summaryWriting);

        // 将数据保存至 Redis
        String key = "Writing" + writings.getId().toString();
        String stringData = objectMapper.writeValueAsString(writings);
        redisTemplate.opsForValue().set(key, stringData);
    }

    /**
     * 获取所有文章摘要
     * @param current 当前页码
     * @return 分页后的文章摘要
     * @throws JsonProcessingException
     */
    @Override
    public Page<SummaryWriting> getBriefWritings(int current) throws JsonProcessingException {
        log.info("开始获取文章摘要");

        // 尝试从 Redis 中获取数据
        String key = "PageSummaryWriting";
        String data = redisTemplate.opsForValue().get(key);

        if (data != null) {
            log.info("使用 Redis 数据获取文章摘要");
            Page resultPage = objectMapper.readValue(data, Page.class);
            return resultPage;
        }
        else {
            log.info("使用 MySQL 数据获取文章摘要");
            Page<SummaryWriting> page = new Page<>(current, 9);
            Page<SummaryWriting> resultPage = summaryWritingMapper.selectPage(page, null);

            // 添加缓存
            String redisData = objectMapper.writeValueAsString(resultPage);
            redisTemplate.opsForValue().set(key, redisData, 38, TimeUnit.MINUTES);
            return resultPage;
        }
    }

    /**
     * 用于模糊查询，即搜索
     * @param keyLike 标题关键字
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @return 分页后的文章摘要
     */
    @Override
    public Page<SummaryWriting> selectWriting(String keyLike, Integer pageNum, Integer pageSize) {
        Page<SummaryWriting> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SummaryWriting> qw = new LambdaQueryWrapper<>();
        qw.like(SummaryWriting::getTitle, keyLike);
        // 可选：添加排序
        qw.orderByDesc(SummaryWriting::getDate);

        Page<SummaryWriting> result = summaryWritingMapper.selectPage(page, qw);
        return result;
    }

    /**
     * 对文章内容进行修改
     * @param writings 文章
     * @param upOrIn 用于标识是插入新文章还是更新旧文章，0 为插入 1 为更新
     * @throws JsonProcessingException
     */
    @Override
    @Transactional
    public void updateWriting(Writings writings, int upOrIn) throws JsonProcessingException {
        log.info("开始对文章进行修改");

        // 更新文章摘要
        // 3. 读取 markdown 内容
        String content = writings.getContent();

        int endIndex = Math.min(content.length(), 60);

        String preview = content.substring(0, endIndex);

        // 更新摘要类
        SummaryWriting summaryWriting = new SummaryWriting();
        summaryWriting.setTitle(writings.getTitle());
        summaryWriting.setPreview(preview);

        // 更新文章类的摘要信息
        writings.setPreview(preview);
        if (upOrIn == 0) {
            writingsMapper.insert(writings);
            // insert 后 MyBatis-Plus 自动回填 writings.id，同步到 summaryWriting 保证两表 ID 一致
            summaryWriting.setId(writings.getId());
            summaryWriting.setDate(LocalDate.now());
            summaryWritingMapper.insert(summaryWriting);
        }
        else if (upOrIn == 1) {
            writingsMapper.updateById(writings);
            summaryWritingMapper.updateById(summaryWriting);
        }
        else {
            throw new IllegalArgumentException("出现未知错误，请联系管理员");
        }

        // 更新在 Redis 中的数据
        String key = "Writing" + writings.getId().toString();
        String rStringData = objectMapper.writeValueAsString(writings);
        redisTemplate.opsForValue().set(key, rStringData);
    }

    /**
     * 用于置顶或取消置顶文章
     * @param id 要置顶的文章的 id
     * @param isTop 1 为置顶，0 为取消置顶
     */
    @Override
    public void topWriting(int id, int isTop) {
        log.info("开始对文章进行置顶或取消置顶，id 为：{}", id);
        String key = "PageSummaryWriting";
        SummaryWriting sw = summaryWritingMapper.selectById(id);
        int oldIsTop = sw.getIsTop();
        if (oldIsTop == isTop && isTop == 1) {
            throw new IllegalArgumentException("该文章已被置顶");
        }
        if (oldIsTop == isTop && isTop == 0) {
            throw new IllegalArgumentException("该文章未被置顶，不能取消置顶");
        }
        sw.setIsTop(isTop);
        summaryWritingMapper.updateById(sw);
        redisTemplate.delete(key);
    }
}
