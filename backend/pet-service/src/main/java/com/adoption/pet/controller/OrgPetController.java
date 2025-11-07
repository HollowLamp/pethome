package com.adoption.pet.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.common.service.FileService;
import com.adoption.common.util.FileUtils;
import com.adoption.pet.model.Pet;
import com.adoption.pet.service.PetService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 机构宠物管理Controller
 * 机构管理员/维护员可访问
 */
@RestController
@RequestMapping("/pets/org")
public class OrgPetController {
    private final PetService petService;
    private final FileService fileService;

    public OrgPetController(PetService petService, FileService fileService) {
        this.petService = petService;
        this.fileService = fileService;
    }

    /**
     * 新增宠物（机构管理员）
     * POST /pets/org
     */
    @PostMapping
    public ApiResponse<Pet> createPet(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Pet pet) {
        return petService.createPet(pet);
    }

    /**
     * 修改宠物信息（机构管理员/维护员）
     * PATCH /pets/org/{id}
     */
    @PatchMapping("/{id}")
    public ApiResponse<Pet> updatePet(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long id,
            @RequestBody Pet pet) {
        return petService.updatePet(id, pet);
    }

    /**
     * 修改宠物状态（机构管理员）
     * POST /pets/org/{id}/status
     */
    @PostMapping("/{id}/status")
    public ApiResponse<String> updatePetStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return petService.updatePetStatus(id, status);
    }

    /**
     * 上传宠物封面图
     * POST /pets/org/{id}/cover
     */
    @PostMapping("/{id}/cover")
    public ApiResponse<Map<String, Object>> uploadCover(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ApiResponse.error(400, "文件不能为空");
            }

            // 检查是否为图片文件
            if (!FileUtils.isImage(file.getOriginalFilename())) {
                return ApiResponse.error(400, "只支持图片文件");
            }

            // 上传文件
            InputStream inputStream = FileUtils.toInputStream(file);
            FileService.FileInfo fileInfo = fileService.uploadFile(
                    inputStream,
                    file.getOriginalFilename(),
                    "pet"  // 宠物封面图分类
            );

            // 更新宠物的封面图URL
            ApiResponse<Pet> updateResult = petService.updatePetCover(id, fileInfo.getRelativePath());

            if (updateResult.getCode() == 200) {
                Map<String, Object> result = new HashMap<>();
                result.put("pet", updateResult.getData());
                result.put("coverUrl", fileInfo.getUrl()); // 返回完整URL
                result.put("relativePath", fileInfo.getRelativePath()); // 返回相对路径
                return ApiResponse.success(result);
            }

            return ApiResponse.error(updateResult.getCode(), updateResult.getMessage());

        } catch (Exception e) {
            return ApiResponse.error(500, "封面图上传失败: " + e.getMessage());
        }
    }
}

