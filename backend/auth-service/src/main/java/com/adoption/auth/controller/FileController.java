package com.adoption.auth.controller;

import com.adoption.common.service.FileService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;

/**
 * 文件访问控制器
 * 提供文件下载和访问功能
 */
@RestController
@RequestMapping("/files")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 访问文件
     * GET /files/{category}/{date}/{filename}
     * 例如：GET /files/user/2025-11-07/d7a9f967-214b-428f-9cd9-6d39dde17f6a.png
     */
    @GetMapping("/**")
    public ResponseEntity<InputStreamResource> getFile(HttpServletRequest request) {
        try {
            // 从请求路径中提取相对路径
            // 请求路径格式：/files/user/2025-11-07/filename.png
            // 需要提取：user/2025-11-07/filename.png
            String requestPath = request.getRequestURI();

            // 移除 /files 前缀
            if (requestPath.startsWith("/files/")) {
                String relativePath = requestPath.substring(7); // 移除 "/files/"

                // 读取文件
                InputStream inputStream = fileService.readFile(relativePath);

                // 获取文件类型
                String filename = relativePath.substring(relativePath.lastIndexOf('/') + 1);
                String contentType = getContentType(filename);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                        .body(new InputStreamResource(inputStream));
            }

            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String getContentType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "application/octet-stream";
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "pdf":
                return "application/pdf";
            case "mp4":
                return "video/mp4";
            default:
                return "application/octet-stream";
        }
    }
}

