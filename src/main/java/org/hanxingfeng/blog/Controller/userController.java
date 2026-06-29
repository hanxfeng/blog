package org.hanxingfeng.blog.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.R;
import org.hanxingfeng.blog.Entity.SummaryWriting;
import org.hanxingfeng.blog.Entity.User;
import org.hanxingfeng.blog.Entity.Writings;
import org.hanxingfeng.blog.Mapper.SummaryWritingMapper;
import org.hanxingfeng.blog.Mapper.UserMapper;
import org.hanxingfeng.blog.Mapper.WritingsMapper;
import org.hanxingfeng.blog.Service.SummaryWritingService;
import org.hanxingfeng.blog.UtilAndOther.JWTUtil;
import org.hanxingfeng.blog.UtilAndOther.UpdateNowData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin")
public class userController {

    @Autowired
    private UpdateNowData updateNowData;

    @Autowired
    private WritingsMapper writingsMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SummaryWritingService summaryWritingService;

    @Autowired
    private SummaryWritingMapper summaryWritingMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // TODO：添加在线写作功能


    /**
     * 用于进行登录
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(HttpServletRequest request, @RequestBody User user) {
        log.info("开始进行登录校验");
        String key = "loginCount";
        String result = redisTemplate.opsForValue().get(key);
        Long count;
        if (result == null) {
            count = 1L;
            redisTemplate.opsForValue().set(key, String.valueOf(count));
        }
        else {
            count = Long.valueOf(result);
            return R.error("尝试次数过多请一小时后再试或联系管理员处理");
        }


        String userName = user.getUserName();
        String password = user.getPassword();

        // 查询数据库中的账号密码
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUserName, userName);
        User sqlUser = userMapper.selectOne(qw);

        if (sqlUser == null || passwordEncoder.matches(password, sqlUser.getPassword())) {
            redisTemplate.opsForValue().set(key, String.valueOf(count + 1));
            return R.error("用户不存在或密码错误！");
        }

        Long userId = sqlUser.getUserId();

        // 生成 token 并返回
        String token = jwtUtil.generateToken(userId, user.getUserName());
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);

        redisTemplate.delete(key);

        return R.success(map);
    }

    /**
     *用于更新今日数据
     * &#064;return：返回是否更新成功
     */
    @GetMapping("/update")
    public R<String> update () throws Exception {
        log.info("开始更新数据");
        updateNowData.updateNoteData();
        return R.success("更新数据成功");
    }

    /**
     * markdown 上传md文件
     */
    @PostMapping("/writingsUpload")
    public R<String> upload(
            @RequestParam("file") MultipartFile file) {

        log.info("开始上传文件");

        Map<String, Object> result = new HashMap<>();

        try {

            // 1. 文件为空校验
            if (file.isEmpty()) {
                return R.error("文件不能为空");
            }

            // 2. 文件后缀校验
            String fileName = file.getOriginalFilename();

            String suffix = StringUtils.getFilenameExtension(fileName);

            if (suffix == null || !suffix.equalsIgnoreCase("md")) {
                return R.error("只能上传 md 文件");
            }

            // 判断是否有同样的文章
            fileName = fileName.substring(0, fileName.length() - 3);
            LambdaQueryWrapper<Writings> qw = new LambdaQueryWrapper<>();
            qw.eq(Writings::getTitle, fileName);
            Writings writingsTemp = writingsMapper.selectOne(qw);
            if (writingsTemp != null) {
                return R.error("已上传过同名文章");
            }

            // 3. 读取 markdown 内容
            String content = new String(
                    file.getBytes(),
                    StandardCharsets.UTF_8
            );

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
            try {
                summaryWritingService.saveWriting(writings, summaryWriting);
            } catch (Exception e) {
                log.info("发生错误：\n {}", e.getMessage());
                return R.error(e.getMessage());
            }

            // 将数据保存至 Redis
            String key = "Writing" + writings.getId().toString();
            String stringData = objectMapper.writeValueAsString(writings);
            redisTemplate.opsForValue().set(key, stringData);

            return R.success("上传成功");

        } catch (Exception e) {

            return R.error(e.getMessage());
        }
    }

    /**
     * 删除指定文章
     */
    @GetMapping("/delete/{id}")
    public R<String> delete(@PathVariable Long id) {
        log.info("开始删除文章，id为：{}",id);

        // 在数据库中删除
        summaryWritingService.deleteWriting(id);

        // 在 Redis 中删除
        String key = "Writing" + id.toString();
        redisTemplate.delete(key);

        return R.success("删除成功");
    }

    /**测试用接口
     *
     * @return
     */
    @GetMapping("/test")
    public R<String> testS () {
        log.info("服务器正常运行");
        return R.success("服务器正常运行");
    }

    /**
     * 对文章内容进行修改
     * @param writings
     * @return
     * @throws JsonProcessingException
     */
    @Transactional
    @PostMapping("/updateWritingById")
    public R<String> updateWritingById(@RequestBody Writings writings) throws JsonProcessingException {
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

        writingsMapper.updateById(writings);
        summaryWritingMapper.updateById(summaryWriting);

        // 更新在 Redis 中的数据
        String key = "Writing" + writings.getId().toString();
        String rStringData = objectMapper.writeValueAsString(writings);
        redisTemplate.opsForValue().set(key, rStringData);



        return R.success("修改成功");
    }

    /**
     * 用于置顶或取消置顶文章
     * @param id 要置顶的文章的 id
     * @param isTop 1 为置顶，0 为取消置顶
     * @return 操作结果
     */
    @GetMapping("/topWriting/{id}/{isTop}")
    public R<String> topWriting(@PathVariable int id, @PathVariable int isTop) {
        try {
            SummaryWriting sw = summaryWritingMapper.selectById(id);
            int oldIsTop = sw.getIsTop();
            if (oldIsTop == isTop && isTop == 1) {
                return R.error("该文章已被置顶");
            }
            if (oldIsTop == isTop && isTop == 0) {
                return R.error("该文章未被置顶，不能取消置顶");
            }
            sw.setIsTop(isTop);
            summaryWritingMapper.updateById(sw);
            return R.success("操作成功");
        }
        catch (Exception e) {
            return R.error("出现错误，请联系管理员处理");
        }

    }

}
