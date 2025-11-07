package com.adoption.pet.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.pet.model.Wishlist;
import com.adoption.pet.model.Pet;
import com.adoption.pet.repository.PetMapper;
import com.adoption.pet.repository.WishlistMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WishlistService {
    private final WishlistMapper wishlistMapper;
    private final PetMapper petMapper;

    public WishlistService(WishlistMapper wishlistMapper, PetMapper petMapper) {
        this.wishlistMapper = wishlistMapper;
        this.petMapper = petMapper;
    }

    /**
     * 加入愿望单
     */
    public ApiResponse<String> addToWishlist(Long userId, Long petId) {
        // 检查宠物是否存在
        if (petMapper.findById(petId) == null) {
            return ApiResponse.error(404, "宠物不存在");
        }

        // 检查是否已在愿望单
        Wishlist existing = wishlistMapper.findByUserAndPet(userId, petId);
        if (existing != null) {
            return ApiResponse.error(400, "已在愿望单中");
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setUserId(userId);
        wishlist.setPetId(petId);
        wishlistMapper.insert(wishlist);

        return ApiResponse.success("已加入愿望单");
    }

    /**
     * 移除愿望单
     */
    public ApiResponse<String> removeFromWishlist(Long userId, Long petId) {
        Wishlist existing = wishlistMapper.findByUserAndPet(userId, petId);
        if (existing == null) {
            return ApiResponse.error(404, "不在愿望单中");
        }

        wishlistMapper.delete(userId, petId);
        return ApiResponse.success("已移除愿望单");
    }

    /**
     * 获取用户的愿望单列表
     */
    public ApiResponse<List<Wishlist>> getUserWishlist(Long userId) {
        List<Wishlist> wishlist = wishlistMapper.findByUserId(userId);
        return ApiResponse.success(wishlist);
    }

    /**
     * 获取用户愿望单中的宠物列表
     */
    public ApiResponse<List<Pet>> getUserWishlistPets(Long userId) {
        List<Wishlist> items = wishlistMapper.findByUserId(userId);
        java.util.List<Pet> pets = new java.util.ArrayList<>();
        for (Wishlist w : items) {
            Pet p = petMapper.findById(w.getPetId());
            if (p != null) {
                pets.add(p);
            }
        }
        return ApiResponse.success(pets);
    }

    /**
     * 检查是否在愿望单中
     */
    public ApiResponse<Boolean> isInWishlist(Long userId, Long petId) {
        Wishlist existing = wishlistMapper.findByUserAndPet(userId, petId);
        return ApiResponse.success(existing != null);
    }
}

