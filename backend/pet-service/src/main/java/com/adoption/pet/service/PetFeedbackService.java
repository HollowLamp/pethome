package com.adoption.pet.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.pet.feign.AuthServiceClient;
import com.adoption.pet.model.Pet;
import com.adoption.pet.model.PetFeedback;
import com.adoption.pet.model.PetFeedbackDTO;
import com.adoption.pet.repository.PetFeedbackMapper;
import com.adoption.pet.repository.PetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PetFeedbackService {
    private static final Logger log = LoggerFactory.getLogger(PetFeedbackService.class);

    private final PetFeedbackMapper petFeedbackMapper;
    private final PetMapper petMapper;
    private final AuthServiceClient authServiceClient;

    public PetFeedbackService(PetFeedbackMapper petFeedbackMapper, PetMapper petMapper, AuthServiceClient authServiceClient) {
        this.petFeedbackMapper = petFeedbackMapper;
        this.petMapper = petMapper;
        this.authServiceClient = authServiceClient;
    }

    /**
     * 查看用户反馈（维护员）
     */
    public ApiResponse<List<PetFeedbackDTO>> getFeedbacks(Long petId) {
        // 检查宠物是否存在
        Pet pet = petMapper.findById(petId);
        if (pet == null) {
            return ApiResponse.error(404, "宠物不存在");
        }

        List<PetFeedback> feedbacks = petFeedbackMapper.findByPetId(petId);
        List<PetFeedbackDTO> result = new ArrayList<>();

        for (PetFeedback feedback : feedbacks) {
            PetFeedbackDTO dto = convertToDTO(feedback);
            // 如果宠物信息还没设置，设置一下
            if (dto.getPetName() == null) {
                dto.setPetName(pet.getName());
            }
            result.add(dto);
        }

        return ApiResponse.success(result);
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
    public ApiResponse<List<PetFeedbackDTO>> getFeedbacksByType(String type) {
        if (type == null || type.isBlank()) {
            return ApiResponse.error(400, "宠物类型不能为空");
        }
        List<PetFeedback> feedbacks = petFeedbackMapper.findByPetType(type);
        List<PetFeedbackDTO> result = new ArrayList<>();

        for (PetFeedback feedback : feedbacks) {
            PetFeedbackDTO dto = convertToDTO(feedback);
            result.add(dto);
        }

        return ApiResponse.success(result);
    }

    /**
     * 将 PetFeedback 转换为 PetFeedbackDTO，并填充用户和宠物信息
     */
    private PetFeedbackDTO convertToDTO(PetFeedback feedback) {
        PetFeedbackDTO dto = new PetFeedbackDTO();
        dto.setId(feedback.getId());
        dto.setPetId(feedback.getPetId());
        dto.setUserId(feedback.getUserId());
        dto.setContent(feedback.getContent());
        dto.setMediaUrls(feedback.getMediaUrls());
        dto.setCreatedAt(feedback.getCreatedAt());

        // 获取宠物信息
        Pet pet = petMapper.findById(feedback.getPetId());
        if (pet != null) {
            dto.setPetName(pet.getName());
        }

        // 获取用户信息
        if (feedback.getUserId() != null) {
            try {
                ApiResponse<Map<String, Object>> userResponse = authServiceClient.getUserById(feedback.getUserId());
                if (userResponse != null && userResponse.getCode() == 200 && userResponse.getData() != null) {
                    Map<String, Object> userData = userResponse.getData();
                    dto.setUsername((String) userData.get("username"));
                    dto.setAvatarUrl((String) userData.get("avatarUrl"));
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败，userId: {}, error: {}", feedback.getUserId(), e.getMessage());
            }
        }

        return dto;
    }
}

