package com.adoption.pet.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.common.service.FileService;
import com.adoption.common.util.FileUtils;
import com.adoption.pet.model.PetFeedback;
import com.adoption.pet.model.PetFeedbackDTO;
import com.adoption.pet.service.PetFeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 宠物反馈Controller
 */
@RestController
@RequestMapping("/pets")
public class PetFeedbackController {
    private final PetFeedbackService petFeedbackService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    public PetFeedbackController(PetFeedbackService petFeedbackService, FileService fileService) {
        this.petFeedbackService = petFeedbackService;
        this.fileService = fileService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 查看用户反馈（维护员）
     * GET /pets/{id}/feedbacks
     */
    @GetMapping("/{id}/feedbacks")
    public ApiResponse<List<PetFeedbackDTO>> getFeedbacks(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("id") Long petId) {
        return petFeedbackService.getFeedbacks(petId);
    }

    /**
     * 创建反馈（支持上传媒体文件）
     * POST /pets/{id}/feedbacks
     */
    @PostMapping("/{id}/feedbacks")
    public ApiResponse<PetFeedback> createFeedback(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long petId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        try {
            List<String> mediaUrls = new ArrayList<>();

            // 处理上传的文件
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        // 检查文件类型（图片或视频）
                        String filename = file.getOriginalFilename();
                        if (!FileUtils.isImage(filename) && !FileUtils.isVideo(filename)) {
                            return ApiResponse.error(400, "只支持图片或视频文件: " + filename);
                        }

                        // 上传文件
                        InputStream inputStream = FileUtils.toInputStream(file);
                        FileService.FileInfo fileInfo = fileService.uploadFile(
                                inputStream,
                                filename,
                                "pet-feedback"  // 反馈媒体文件分类
                        );

                        mediaUrls.add(fileInfo.getUrl()); // 存储完整URL
                    }
                }
            }

            // 将媒体URL列表转换为JSON字符串
            String mediaUrlsJson = objectMapper.writeValueAsString(mediaUrls);

            return petFeedbackService.createFeedback(petId, userId, content, mediaUrlsJson);

        } catch (Exception e) {
            return ApiResponse.error(500, "创建反馈失败: " + e.getMessage());
        }
    }

    /**
     * 根据宠物类型查看反馈（登录用户）
     * GET /pets/type/{type}/feedbacks
     */
    @GetMapping("/type/{type}/feedbacks")
    public ApiResponse<List<PetFeedbackDTO>> getFeedbacksByType(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("type") String type) {
        return petFeedbackService.getFeedbacksByType(type);
    }
}

