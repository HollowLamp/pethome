package com.adoption.pet.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.pet.model.PetHealth;
import com.adoption.pet.repository.PetHealthMapper;
import com.adoption.pet.repository.PetMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PetHealthService {
    private final PetHealthMapper petHealthMapper;
    private final PetMapper petMapper;

    public PetHealthService(PetHealthMapper petHealthMapper, PetMapper petMapper) {
        this.petHealthMapper = petHealthMapper;
        this.petMapper = petMapper;
    }

    /**
     * 更新健康/疫苗记录（维护员）
     */
    public ApiResponse<PetHealth> updateHealth(Long petId, PetHealth health, Long updatedBy) {
        // 检查宠物是否存在
        if (petMapper.findById(petId) == null) {
            return ApiResponse.error(404, "宠物不存在");
        }

        health.setPetId(petId);
        health.setUpdatedBy(updatedBy);

        // 检查是否已有记录
        PetHealth existing = petHealthMapper.findLatestByPetId(petId);
        if (existing != null) {
            // 更新现有记录
            petHealthMapper.updateByPetId(health);
        } else {
            // 创建新记录
            petHealthMapper.insert(health);
        }

        PetHealth updated = petHealthMapper.findLatestByPetId(petId);
        return ApiResponse.success(updated);
    }

    /**
     * 获取健康记录
     */
    public ApiResponse<PetHealth> getHealth(Long petId) {
        PetHealth health = petHealthMapper.findLatestByPetId(petId);
        if (health == null) {
            return ApiResponse.error(404, "暂无健康记录");
        }
        return ApiResponse.success(health);
    }

    /**
     * 获取所有健康记录历史
     */
    public ApiResponse<List<PetHealth>> getHealthHistory(Long petId) {
        List<PetHealth> history = petHealthMapper.findAllByPetId(petId);
        return ApiResponse.success(history);
    }
}

