package com.adoption.pet.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.pet.model.Pet;
import com.adoption.pet.service.WishlistService;
import org.springframework.web.bind.annotation.*;

/**
 * 愿望单Controller
 * 登录用户可访问
 */
@RestController
@RequestMapping("/pets")
public class WishlistController {
    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    /**
     * 加入愿望单
     * POST /pets/{id}/wishlist
     */
    @PostMapping("/{id}/wishlist")
    public ApiResponse<String> addToWishlist(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long petId) {
        return wishlistService.addToWishlist(userId, petId);
    }

    /**
     * 移除愿望单
     * DELETE /pets/{id}/wishlist
     */
    @DeleteMapping("/{id}/wishlist")
    public ApiResponse<String> removeFromWishlist(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long petId) {
        return wishlistService.removeFromWishlist(userId, petId);
    }

    /**
     * 获取当前用户愿望单中的宠物列表
     * GET /pets/wishlist
     */
    @GetMapping("/wishlist")
    public ApiResponse<java.util.List<Pet>> getWishlistPets(
            @RequestHeader("X-User-Id") Long userId) {
        return wishlistService.getUserWishlistPets(userId);
    }
}

