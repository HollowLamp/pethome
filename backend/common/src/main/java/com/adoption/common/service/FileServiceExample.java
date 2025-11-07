package com.adoption.common.service;

import com.adoption.common.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件服务使用示例
 * 这个类仅作为示例，展示如何在业务服务中使用FileService
 *
 * 使用步骤：
 * 1. 在业务服务的Controller中注入FileService
 * 2. 接收MultipartFile参数
 * 3. 转换为InputStream后调用FileService.uploadFile()
 * 4. 将返回的FileInfo中的relativePath存储到数据库
 * 5. 前端使用FileInfo中的url来展示文件
 *
 * 示例代码：
 */
// @RestController
// @RequestMapping("/api/pet")
public class FileServiceExample {

    @Autowired
    private FileService fileService;

    /**
     * 上传宠物图片示例
     */
    // @PostMapping("/upload-image")
    public FileService.FileInfo uploadPetImage(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 验证文件
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("文件不能为空");
            }

            // 2. 检查文件类型（可选）
            if (!FileUtils.isImage(file.getOriginalFilename())) {
                throw new RuntimeException("只支持图片文件");
            }

            // 3. 转换为InputStream并上传
            InputStream inputStream = FileUtils.toInputStream(file);
            FileService.FileInfo fileInfo = fileService.uploadFile(
                inputStream,
                file.getOriginalFilename(),
                "pet"  // 文件分类：pet表示宠物相关文件
            );

            // 4. 返回文件信息，业务模块可以将relativePath存储到数据库
            // 例如：pet表的image_url字段存储 fileInfo.getRelativePath()
            // 前端展示时使用 fileInfo.getUrl()

            return fileInfo;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件示例
     */
    // @DeleteMapping("/file/{relativePath}")
    public void deleteFile(@PathVariable String relativePath) {
        // 从数据库查询到文件的relativePath后，调用删除
        fileService.deleteFile(relativePath);
    }

    /**
     * 读取文件示例（用于文件下载）
     */
    // @GetMapping("/file/{relativePath}")
    public InputStream downloadFile(@PathVariable String relativePath) {
        // 从数据库查询到文件的relativePath后，返回文件流
        return fileService.readFile(relativePath);
    }
}

