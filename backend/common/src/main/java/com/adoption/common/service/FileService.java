package com.adoption.common.service;

import com.adoption.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件服务
 * 提供文件上传、下载、删除等功能
 * 不管理数据库，只负责文件存储，返回文件路径供业务模块存储
 */
@Service
public class FileService {
    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.upload.url-prefix:/files}")
    private String urlPrefix;

    @Value("${file.upload.max-size:10485760}") // 默认10MB
    private long maxFileSize;

    /**
     * 上传文件
     * @param inputStream 文件输入流
     * @param originalFilename 原始文件名
     * @param category 文件分类（如：pet, org, user等），用于组织文件目录
     * @return 文件信息（包含相对路径和访问URL）
     */
    public FileInfo uploadFile(InputStream inputStream, String originalFilename, String category) {
        if (inputStream == null) {
            throw new BusinessException(400, "文件流不能为空");
        }
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(400, "文件名不能为空");
        }
        if (!StringUtils.hasText(category)) {
            category = "default";
        }

        try {
            // 生成文件路径：category/yyyy-MM-dd/uuid.extension
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String extension = getFileExtension(originalFilename);
            String filename = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);

            Path categoryPath = Paths.get(uploadPath, category, dateDir);
            Files.createDirectories(categoryPath);

            Path filePath = categoryPath.resolve(filename);

            // 保存文件
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            // 获取实际文件大小
            long fileSize = Files.size(filePath);

            // 检查文件大小
            if (fileSize > maxFileSize) {
                // 如果超过限制，删除已保存的文件
                Files.deleteIfExists(filePath);
                throw new BusinessException(400, "文件大小超过限制：" + (maxFileSize / 1024 / 1024) + "MB");
            }

            // 构建相对路径和URL
            String relativePath = Paths.get(category, dateDir, filename).toString().replace("\\", "/");
            String url = urlPrefix + "/" + relativePath;

            log.info("文件上传成功: {}", relativePath);

            return new FileInfo(
                relativePath,
                url,
                originalFilename,
                filename,
                fileSize,
                getContentType(extension)
            );

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(500, "文件上传失败: " + e.getMessage());
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                log.warn("关闭文件流失败", e);
            }
        }
    }

    /**
     * 读取文件
     * @param relativePath 相对路径（如：pet/2024-01-01/uuid.jpg）
     * @return 文件输入流
     */
    public InputStream readFile(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new BusinessException(400, "文件路径不能为空");
        }

        try {
            Path filePath = Paths.get(uploadPath, relativePath);
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new BusinessException(404, "文件不存在: " + relativePath);
            }

            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("读取文件失败: {}", relativePath, e);
            throw new BusinessException(500, "读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     * @param relativePath 相对路径
     */
    public void deleteFile(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return;
        }

        try {
            Path filePath = Paths.get(uploadPath, relativePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文件删除成功: {}", relativePath);
            }
        } catch (IOException e) {
            log.error("删除文件失败: {}", relativePath, e);
            throw new BusinessException(500, "删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 检查文件是否存在
     * @param relativePath 相对路径
     * @return 是否存在
     */
    public boolean fileExists(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return false;
        }

        Path filePath = Paths.get(uploadPath, relativePath);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

    /**
     * 获取文件大小
     * @param relativePath 相对路径
     * @return 文件大小（字节）
     */
    public long getFileSize(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return 0;
        }

        try {
            Path filePath = Paths.get(uploadPath, relativePath);
            if (Files.exists(filePath)) {
                return Files.size(filePath);
            }
        } catch (IOException e) {
            log.error("获取文件大小失败: {}", relativePath, e);
        }
        return 0;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 根据扩展名获取Content-Type
     */
    private String getContentType(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "application/octet-stream";
        }

        switch (extension.toLowerCase()) {
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
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
            case "mov":
                return "video/quicktime";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * 文件信息DTO
     */
    public static class FileInfo {
        private String relativePath;  // 相对路径，用于存储到数据库
        private String url;           // 访问URL，用于前端展示
        private String originalFilename; // 原始文件名
        private String filename;      // 存储的文件名
        private long size;            // 文件大小（字节）
        private String contentType;   // 文件类型

        public FileInfo(String relativePath, String url, String originalFilename,
                       String filename, long size, String contentType) {
            this.relativePath = relativePath;
            this.url = url;
            this.originalFilename = originalFilename;
            this.filename = filename;
            this.size = size;
            this.contentType = contentType;
        }

        // Getters
        public String getRelativePath() { return relativePath; }
        public String getUrl() { return url; }
        public String getOriginalFilename() { return originalFilename; }
        public String getFilename() { return filename; }
        public long getSize() { return size; }
        public String getContentType() { return contentType; }
    }
}

