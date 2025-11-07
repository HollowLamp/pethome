package com.adoption.common.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件工具类
 * 提供文件相关的工具方法
 */
public class FileUtils {

    /**
     * 将MultipartFile转换为InputStream
     * 注意：调用者需要负责关闭InputStream
     */
    public static InputStream toInputStream(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return file.getInputStream();
    }

    /**
     * 获取文件扩展名
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 检查是否为图片文件
     */
    public static boolean isImage(String filename) {
        String extension = getFileExtension(filename);
        return extension.matches("jpg|jpeg|png|gif|webp|bmp");
    }

    /**
     * 检查是否为视频文件
     */
    public static boolean isVideo(String filename) {
        String extension = getFileExtension(filename);
        return extension.matches("mp4|avi|mov|wmv|flv|mkv");
    }

    /**
     * 检查文件大小是否在限制内
     */
    public static boolean isSizeValid(long fileSize, long maxSize) {
        return fileSize > 0 && fileSize <= maxSize;
    }

    /**
     * 格式化文件大小（字节转可读格式）
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}

