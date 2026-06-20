package org.hanxingfeng.blog.Controller;


import lombok.extern.slf4j.Slf4j;
import org.hanxingfeng.blog.Entity.R;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.hanxingfeng.blog.Entity.SystemConstants.UPLOAD_DIR;

@Slf4j
@RestController
@RequestMapping("/file")
public class commonController {
    /**
     * 图片上传
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file) {
        log.info("开始文件上传");
        Map<String, Object> result = new HashMap<>();

        try {

            // 判断文件是否为空
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("msg", "上传文件不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            // 创建目录
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();

            // 获取后缀
            String suffix = StringUtils.getFilenameExtension(originalFilename);

            if (!Objects.equals(suffix, "png") && !Objects.equals(suffix, "jpg")) {
                result.put("success", false);
                result.put("msg", "只能上传 jpg 或 png 文件");
                return ResponseEntity.badRequest().body(result);
            }

            // 生成新文件名
            LocalDate date = LocalDate.now();
            String newFileName = date.toString() + UUID.randomUUID() + "." + suffix;

            // 保存文件
            Path path = Paths.get(UPLOAD_DIR + newFileName);
            Files.copy(file.getInputStream(), path);

            result.put("success", true);
            result.put("msg", "上传成功");
            result.put("fileName", newFileName);

            // 返回下载地址
            result.put("url",
                    "http://localhost:8080/file/download/" + newFileName);

            return ResponseEntity.ok(result);

        } catch (Exception e) {

            result.put("success", false);
            result.put("msg", e.getMessage());

            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 图片下载 / 预览
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> download(
            @PathVariable String fileName,
            HttpServletRequest request) throws IOException {

        File file = new File(UPLOAD_DIR + fileName);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        // 自动识别文件类型
        String contentType = request.getServletContext()
                .getMimeType(file.getAbsolutePath());

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getName() + "\"")
                .body(resource);
    }


    /**
     * 修改头像图片/背景图片/登录页图片
     */
    @PostMapping("/setImage")
    public ResponseEntity<Map<String, Object>> setImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) throws IOException {
        Map<String, Object> result = new HashMap<>();

        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();

        // 获取文件后缀
        String suffix = StringUtils.getFilenameExtension(originalFilename);

        // 进行文件格式校验
        if (!Objects.equals(suffix, "png") && !Objects.equals(suffix, "jpg")) {
            result.put("success", false);
            result.put("msg", "只能上传 jpg 或 png 文件");
            return ResponseEntity.badRequest().body(result);
        }

        // 生成文件保存路径
        String filePath = "E:\\obsdian\\blog\\src\\main\\resources\\static\\images\\";

        // 生成新文件名
        String fileName;
        if (Objects.equals(type, "AVATAR")) {
            fileName = "头像." + suffix;
        }
        else if (Objects.equals(type, "BACKGROUND")) {
            fileName = "背景." + suffix;
        }
        else if(Objects.equals(type, "LOGIN_BG")) {
            fileName = "登录页背景." + suffix;
        }
        else {
            result.put("success", false);
            result.put("msg", "type 错误");
            return ResponseEntity.badRequest().body(result);
        }


        try {
            // 检查目录是否存在，不存在则创建目录
            File dir = new File(filePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 保存文件
            Path path = Paths.get(filePath + fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            result.put("success", true);
            result.put("msg", "上传成功");
            result.put("fileName", fileName);

            return ResponseEntity.ok(result);
        }
        catch (Exception e) {

            result.put("success", false);
            result.put("msg", e.getMessage());

            return ResponseEntity.internalServerError().body(result);
        }
    }
}