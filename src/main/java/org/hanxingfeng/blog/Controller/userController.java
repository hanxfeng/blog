package org.hanxingfeng.blog.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.R;
import org.hanxingfeng.blog.Entity.User;
import org.hanxingfeng.blog.Entity.Writings;
import org.hanxingfeng.blog.Service.NodeService;
import org.hanxingfeng.blog.Service.SummaryWritingService;
import org.hanxingfeng.blog.Service.UserService;
import org.hanxingfeng.blog.UtilAndOther.JWTUtil;
import org.hanxingfeng.blog.UtilAndOther.UpdateNowData;
import org.hanxingfeng.blog.UtilAndOther.getGitHubCommit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
// TODO:对管理员界面的页面也进行相应更新
@Slf4j
@RestController
@RequestMapping("/admin")
public class userController {

    @Autowired
    private UpdateNowData updateNowData;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SummaryWritingService summaryWritingService;

    @Autowired
    private UserService userService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private getGitHubCommit getGitHubCommit;

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
            redisTemplate.opsForValue().set(key, String.valueOf(count), 1, TimeUnit.HOURS);
        }
        else {
            count = Long.valueOf(result);
            if (count >= 5) {
                return R.error("尝试次数过多请一小时后再试或联系管理员处理");
            }}


        String userName = user.getUserName();
        String password = user.getPassword();

        // 查询数据库中的账号密码并进行校验
        User sqlUser;
        try {
            sqlUser = userService.login(userName, password);
        }
        catch (IllegalArgumentException e) {
            redisTemplate.opsForValue().set(key, String.valueOf(count + 1));
            return R.error(e.getMessage());
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
     *用于更新昨日数据
     * &#064;return：返回是否更新成功
     */
    @GetMapping("/update")
    public R<String> update () throws Exception {
        log.info("开始更新数据");
        updateNowData.updateNoteData();
        getGitHubCommit.CommitStatService();
        return R.success("更新数据成功");
    }

    /**
     * markdown 上传md文件
     */
    @PostMapping("/writingsUpload")
    public R<String> upload(
            @RequestParam("file") MultipartFile file) {

        log.info("开始上传文件");

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

            // 3. 读取 markdown 内容
            String content = new String(
                    file.getBytes(),
                    StandardCharsets.UTF_8
            );

            // 4. 将数据保存至数据库和 Redis
            summaryWritingService.uploadWriting(fileName, content);

            return R.success("上传成功");

        } catch (Exception e) {

            log.info("发生错误：\n {}", e.getMessage());

            return R.error(e.getMessage());
        }
    }

    /**
     * 删除指定文章
     */
    @GetMapping("/delete/{id}")
    public R<String> delete(@PathVariable Long id) {
        log.info("开始删除文章，id为：{}",id);

        // 在数据库和 Redis 中删除
        summaryWritingService.deleteWriting(id);

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
     * @param upOrIn 用于标识是插入新文章还是更新旧文章，0 为插入 1 为更新
     * @return
     * @throws JsonProcessingException
     */
    @PostMapping("/updateWritingById")
    public R<String> updateWritingById(@RequestBody Writings writings
    , @RequestParam(value="upOrIn", required = false) Integer upOrIn) throws JsonProcessingException {

        try {
            summaryWritingService.updateWriting(writings, upOrIn == null ? 0 : upOrIn);
        }
        catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        }

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
            summaryWritingService.topWriting(id, isTop);
            return R.success("操作成功");
        }
        catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        }
        catch (Exception e) {
            return R.error("出现错误，请联系管理员处理");
        }

    }

    /**
     * 笔记单文件上传（手动指定路径）
     */
    @PostMapping("/nodeUpload")
    public R<String> nodeUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "path", required = false) String path) {

        if (file.isEmpty()) {
            return R.error("文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String suffix = StringUtils.getFilenameExtension(originalName);
        if (suffix == null || !suffix.equalsIgnoreCase("md")) {
            return R.error("只能上传 md 文件");
        }

        try {
            String body = new String(file.getBytes(), StandardCharsets.UTF_8);
            nodeService.uploadNode(originalName, path, body);
            return R.success("上传成功");
        } catch (Exception e) {
            log.error("上传笔记失败: {}", e.getMessage());
            return R.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 笔记文件夹批量上传（从 webkitRelativePath 提取路径）
     */
    @PostMapping("/nodeBatchUpload")
    public R<String> nodeBatchUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("paths") String pathsJson) {

        if (files == null || files.length == 0) {
            return R.error("文件不能为空");
        }

        try {
            String[] paths = objectMapper.readValue(pathsJson, String[].class);
            int inserted = 0, updated = 0;

            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                String relativePath = paths[i].replace('\\', '/');

                String suffix = StringUtils.getFilenameExtension(relativePath);
                if (suffix == null || !suffix.equalsIgnoreCase("md")) {
                    continue;
                }

                String body = new String(file.getBytes(), StandardCharsets.UTF_8);

                boolean isUpdate = nodeService.saveOrUpdateNode(relativePath, body);
                if (isUpdate) updated++;
                else inserted++;
            }

            log.info("批量上传完成: 新增 {} 篇, 更新 {} 篇", inserted, updated);
            return R.success("上传完成: 新增 " + inserted + " 篇, 更新 " + updated + " 篇");
        } catch (Exception e) {
            log.error("批量上传失败: {}", e.getMessage());
            return R.error("批量上传失败: " + e.getMessage());
        }
    }

}
