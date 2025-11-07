package com.adoption.pet.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.pet.model.Pet;
import com.adoption.pet.service.PetService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 宠物Controller
 * 游客/用户可访问的接口
 */
@RestController
@RequestMapping("/pets")
public class PetController {
    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    /**
     * 获取宠物列表（分页+筛选）
     * GET /pets?page=1&pageSize=10&type=DOG&status=AVAILABLE
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getPetList(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return petService.getPetList(type, status, orgId, page, pageSize);
    }

    /**
     * 获取宠物详情
     * GET /pets/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<Pet> getPetById(@PathVariable("id") Long id) {
        return petService.getPetById(id);
    }
}

