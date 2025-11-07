package com.adoption.pet.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.pet.model.PetFeedback;
import com.adoption.pet.repository.PetFeedbackMapper;
import com.adoption.pet.repository.PetMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PetFeedbackService {
    private final PetFeedbackMapper petFeedbackMapper;
    private final PetMapper petMapper;

    public PetFeedbackService(PetFeedbackMapper petFeedbackMapper, PetMapper petMapper) {
        this.petFeedbackMapper = petFeedbackMapper;
        this.petMapper = petMapper;
    }

    /**
     * 查看用户反馈（维护员）
     */
    public ApiResponse<List<PetFeedback>> getFeedbacks(Long petId) {
        // 检查宠物是否存在
        if (petMapper.findById(petId) == null) {
            return ApiResponse.error(404, "宠物不存在");
        }

        List<PetFeedback> feedbacks = petFeedbackMapper.findByPetId(petId);
        return ApiResponse.success(feedbacks);
    }

    /**
     * 创建反馈（用户）
     */
    public ApiResponse<PetFeedback> createFeedback(Long petId, Long userId, String content, String mediaUrls) {
        // 检查宠物是否存在
        if (petMapper.findById(petId) == null) {
            return ApiResponse.error(404, "宠物不存在");
        }

        PetFeedback feedback = new PetFeedback();
        feedback.setPetId(petId);
        feedback.setUserId(userId);
        feedback.setContent(content);
        feedback.setMediaUrls(mediaUrls);

        petFeedbackMapper.insert(feedback);
        return ApiResponse.success(feedback);
    }

    /**
     * 根据宠物类型查看用户反馈（登录用户）
     */
    public ApiResponse<java.util.List<PetFeedback>> getFeedbacksByType(String type) {
        if (type == null || type.isBlank()) {
            return ApiResponse.error(400, "宠物类型不能为空");
        }
        java.util.List<PetFeedback> list = petFeedbackMapper.findByPetType(type);
        return ApiResponse.success(list);
    }
}

