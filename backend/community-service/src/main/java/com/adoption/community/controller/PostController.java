package com.adoption.community.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.common.service.FileService;
import com.adoption.common.util.FileUtils;
import com.adoption.common.util.UserContext;
import com.adoption.community.model.Post;
import com.adoption.community.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 帖子控制器
 * 
 * 作用：处理帖子相关的HTTP请求
 * 
 * 主要功能：
 * - 帖子列表查询（支持类型筛选、排序、分页）
 * - 帖子详情查询
 * - 发布帖子
 * - 删除帖子
 * - 我的帖子查询
 * - 文件上传（单个和批量）
 * 
 * 文件上传功能说明：
 * - 支持图片和视频文件
 * - 单个文件上传：POST /posts/upload
 * - 批量文件上传：POST /posts/upload/batch（最多9个文件）
 * - 上传后返回文件URL，用于帖子发布时存储到mediaUrls字段
 */
@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserContext userContext;

    @Autowired
    private FileService fileService;

    /**
     * 获取帖子列表（支持 type、sort、page）
     * GET /posts
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getPostList(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize) {
        return postService.getPostList(type, sort, page, pageSize);
    }

    /**
     * 获取帖子详情
     * GET /posts/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<Post> getPostById(@PathVariable("id") Long id) {
        return postService.getPostById(id);
    }

    /**
     * 发布帖子
     * POST /posts
     */
    @PostMapping
    public ApiResponse<Post> createPost(@RequestBody Post post) {
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        return postService.createPost(post, currentUserId);
    }

    /**
     * 删除自己的帖子
     * DELETE /posts/{id}
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deletePost(@PathVariable("id") Long id) {
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        return postService.deletePost(id, currentUserId);
    }

    /**
     * 获取我发布的帖子
     * GET /posts/my
     */
    @GetMapping("/my")
    public ApiResponse<Map<String, Object>> getMyPosts(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "pageSize", required = false) Integer pageSize) {
        Long currentUserId = userContext.getCurrentUserId();
        if (currentUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        return postService.getMyPosts(currentUserId, page, pageSize);
    }

    /**
     * 上传文件（单个文件）
     * 
     * 接口路径：POST /posts/upload
     * 
     * 功能说明：
     * - 上传单个图片或视频文件
     * - 支持的文件类型：图片（jpg、png、gif等）和视频（mp4、avi等）
     * - 文件会被上传到文件服务器，返回可访问的URL
     * - 返回的文件URL用于帖子发布时存储到mediaUrls字段（JSON数组格式）
     * 
     * 使用流程：
     * 1. 前端调用此接口上传文件，获取文件URL
     * 2. 将文件URL添加到mediaUrls数组中
     * 3. 发布帖子时，将mediaUrls数组转为JSON字符串存储
     * 
     * 技术实现：
     * - 使用MultipartFile接收文件
     * - 通过FileUtils验证文件类型
     * - 通过FileService上传到文件服务器（可能是本地存储或OSS）
     * - 返回文件信息（URL、相对路径、文件名、大小、内容类型）
     * 
     * @param file 上传的文件（multipart/form-data格式）
     * @return 包含文件URL、相对路径、文件名、大小、内容类型的响应
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件：检查文件是否为空
            if (file == null || file.isEmpty()) {
                return ApiResponse.error(400, "文件不能为空");
            }

            // 检查文件类型：只支持图片和视频文件
            // FileUtils.isImage() 检查是否为图片（jpg、png、gif等）
            // FileUtils.isVideo() 检查是否为视频（mp4、avi等）
            String filename = file.getOriginalFilename();
            if (!FileUtils.isImage(filename) && !FileUtils.isVideo(filename)) {
                return ApiResponse.error(400, "只支持图片或视频文件");
            }

            // 上传文件到文件服务器
            // 1. 将MultipartFile转换为InputStream
            // 2. 调用FileService.uploadFile()上传文件
            // 3. "community" 是文件分类，用于组织文件存储路径
            InputStream inputStream = FileUtils.toInputStream(file);
            FileService.FileInfo fileInfo = fileService.uploadFile(
                    inputStream,
                    filename,
                    "community"  // 社区文件分类，文件会存储在community目录下
            );

            Map<String, Object> result = new HashMap<>();
            result.put("url", fileInfo.getUrl());
            result.put("relativePath", fileInfo.getRelativePath());
            result.put("filename", fileInfo.getFilename());
            result.put("size", fileInfo.getSize());
            result.put("contentType", fileInfo.getContentType());

            return ApiResponse.success(result);

        } catch (Exception e) {
            return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件（批量上传）
     * 
     * 接口路径：POST /posts/upload/batch
     * 
     * 功能说明：
     * - 批量上传多个图片或视频文件（最多9个）
     * - 支持混合上传（图片和视频可以一起上传）
     * - 逐个处理文件，部分失败不影响其他文件
     * - 返回成功上传的文件列表和错误信息（如果有）
     * 
     * 使用场景：
     * - 发布包含多张图片的帖子
     * - 发布包含图片和视频的帖子
     * 
     * 技术实现：
     * - 接收MultipartFile数组
     * - 遍历数组，逐个验证和上传
     * - 收集成功和失败的结果
     * - 如果所有文件都失败，返回错误；否则返回部分成功的结果
     * 
     * @param files 上传的文件数组（multipart/form-data格式，字段名为files）
     * @return 包含成功文件列表、成功数量、总数量、错误信息的响应
     */
    @PostMapping("/upload/batch")
    public ApiResponse<Map<String, Object>> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        try {
            // 验证文件数组：检查是否为空
            if (files == null || files.length == 0) {
                return ApiResponse.error(400, "文件不能为空");
            }

            // 限制最多上传9个文件（防止请求过大）
            if (files.length > 9) {
                return ApiResponse.error(400, "最多只能上传9个文件");
            }

            // 用于存储成功上传的文件信息
            List<Map<String, Object>> fileList = new ArrayList<>();
            // 用于存储上传失败的错误信息
            List<String> errors = new ArrayList<>();

            // 逐个上传文件：遍历文件数组，逐个处理
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                try {
                    // 验证单个文件：检查文件是否为空
                    if (file == null || file.isEmpty()) {
                        errors.add("文件 " + (i + 1) + " 为空");
                        continue;  // 跳过当前文件，继续处理下一个
                    }

                    // 检查文件类型：只支持图片和视频
                    String filename = file.getOriginalFilename();
                    if (!FileUtils.isImage(filename) && !FileUtils.isVideo(filename)) {
                        errors.add("文件 " + (i + 1) + " 格式不支持（只支持图片或视频）");
                        continue;  // 跳过当前文件，继续处理下一个
                    }

                    // 上传文件到文件服务器
                    InputStream inputStream = FileUtils.toInputStream(file);
                    FileService.FileInfo fileInfo = fileService.uploadFile(
                            inputStream,
                            filename,
                            "community"  // 社区文件分类
                    );

                    Map<String, Object> fileData = new HashMap<>();
                    fileData.put("url", fileInfo.getUrl());
                    fileData.put("relativePath", fileInfo.getRelativePath());
                    fileData.put("filename", fileInfo.getFilename());
                    fileData.put("size", fileInfo.getSize());
                    fileData.put("contentType", fileInfo.getContentType());
                    fileList.add(fileData);

                } catch (Exception e) {
                    errors.add("文件 " + (i + 1) + " 上传失败: " + e.getMessage());
                }
            }

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("files", fileList);  // 成功上传的文件列表
            result.put("successCount", fileList.size());  // 成功数量
            result.put("totalCount", files.length);  // 总数量
            if (!errors.isEmpty()) {
                result.put("errors", errors);  // 错误信息（如果有）
            }

            // 如果所有文件都上传失败，返回错误
            // 否则返回部分成功的结果（前端可以根据errors判断哪些文件失败了）
            if (fileList.isEmpty()) {
                return ApiResponse.error(500, "所有文件上传失败: " + String.join(", ", errors));
            }

            return ApiResponse.success(result);

        } catch (Exception e) {
            return ApiResponse.error(500, "批量文件上传失败: " + e.getMessage());
        }
    }
}

