package org.hanxingfeng.blog.Controller;


import lombok.extern.slf4j.Slf4j;
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
import java.util.HashMap;
import java.util.Map;
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

            // 生成新文件名
            String newFileName = UUID.randomUUID() + "." + suffix;

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
}