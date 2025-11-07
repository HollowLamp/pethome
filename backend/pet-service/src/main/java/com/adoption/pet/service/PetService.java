package com.adoption.pet.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.pet.model.Pet;
import com.adoption.pet.repository.PetMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PetService {
    private final PetMapper petMapper;

    public PetService(PetMapper petMapper) {
        this.petMapper = petMapper;
    }

    /**
     * 获取宠物列表（分页+筛选）
     */
    public ApiResponse<Map<String, Object>> getPetList(String type, String status, Long orgId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Pet> pets = petMapper.findAll(type, status, orgId, offset, pageSize);
        int total = petMapper.countAll(type, status, orgId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", pets);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ApiResponse.success(result);
    }

    /**
     * 获取宠物详情
     */
    public ApiResponse<Pet> getPetById(Long id) {
        Pet pet = petMapper.findById(id);
        if (pet == null) {
            return ApiResponse.error(404, "宠物不存在");
        }
        return ApiResponse.success(pet);
    }

    /**
     * 新增宠物（机构管理员）
     */
    public ApiResponse<Pet> createPet(Pet pet) {
        if (pet.getStatus() == null) {
            pet.setStatus("AVAILABLE");
        }
        petMapper.insert(pet);
        return ApiResponse.success(pet);
    }

    /**
     * 修改宠物信息（机构管理员/维护员）
     */
    public ApiResponse<Pet> updatePet(Long id, Pet pet) {
        Pet existing = petMapper.findById(id);
        if (existing == null) {
            return ApiResponse.error(404, "宠物不存在");
        }

        pet.setId(id);
        petMapper.update(pet);

        Pet updated = petMapper.findById(id);
        return ApiResponse.success(updated);
    }

    /**
     * 修改宠物状态（机构管理员）
     */
    public ApiResponse<String> updatePetStatus(Long id, String status) {
        Pet existing = petMapper.findById(id);
        if (existing == null) {
            return ApiResponse.error(404, "宠物不存在");
        }

        petMapper.updateStatus(id, status);
        return ApiResponse.success("状态更新成功");
    }

    /**
     * 更新宠物封面图
     */
    public ApiResponse<Pet> updatePetCover(Long id, String coverUrl) {
        Pet existing = petMapper.findById(id);
        if (existing == null) {
            return ApiResponse.error(404, "宠物不存在");
        }

        Pet pet = new Pet();
        pet.setId(id);
        pet.setCoverUrl(coverUrl);
        petMapper.update(pet);

        Pet updated = petMapper.findById(id);
        return ApiResponse.success(updated);
    }
}

