package org.hanxingfeng.blog.Service.ServiceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.Writings;
import org.hanxingfeng.blog.Mapper.WritingsMapper;
import org.hanxingfeng.blog.Service.WritingsService;
import org.hanxingfeng.blog.UtilAndOther.ZipUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WritingsServiceImpl extends ServiceImpl<WritingsMapper, Writings> implements WritingsService {

    @Autowired
    private WritingsMapper writingsMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取指定文章，并将点击量加一
     * @param id 文章的 id
     * @return 文章
     * @throws JsonProcessingException
     */
    @Override
    public Writings getWritingsById(Long id) throws JsonProcessingException {
        log.info("获取id为：{}的文章",id);
        String key = "WritingCache:" + id;
        String fieldMeat ="meat";
        String fieldContent = "content";
        Map<Object, Object> cache = redisTemplate.opsForHash().entries(key);

        Writings writings;
        String content;


        if (!cache.isEmpty() && cache.containsKey(fieldMeat) && cache.containsKey(fieldContent)) {
            log.info("使用 Redis 获取数据");
            String metaJson = (String) cache.get(fieldMeat);
            byte[] contentByte = (byte[]) cache.get(fieldContent);
            writings = objectMapper.readValue(metaJson, Writings.class);
            content = ZipUtil.decompress(contentByte);
        }
        else {
            if (cache.get("null").equals("null")) {
                log.info("返回 null");
                return null;
            }
            log.info("使用 MySQL 获取数据");
            writings = writingsMapper.selectById(id);
            if (writings == null) {
                Map<String, Object> errorMap = new HashMap<String, Object>();
                errorMap.put("null", "null");
                redisTemplate.opsForHash().putAll(key, errorMap);
                redisTemplate.expire(key, 34, TimeUnit.MINUTES);
                return  null;
            }
            content = writings.getContent();
        }

        writings.setViewCount(writings.getViewCount() + 1);
        writingsMapper.updateById(writings);

        // 写回缓存
        Writings cacheWriting = new Writings();
        BeanUtils.copyProperties(writings, cacheWriting);
        cacheWriting.setContent("");
        String metaJson = objectMapper.writeValueAsString(cacheWriting);
        byte[] compressedContent = ZipUtil.compress(content);

        Map<String, Object> map = new HashMap<>();
        map.put(fieldMeat, metaJson);
        map.put(fieldContent, compressedContent);
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, 40, TimeUnit.MINUTES);

        writings.setContent(content);
        return writings;
    }
}
