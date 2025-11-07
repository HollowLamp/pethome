package com.adoption.pet.repository;

import com.adoption.pet.model.Wishlist;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WishlistMapper {

    @Select("SELECT id, user_id AS userId, pet_id AS petId, created_at AS createdAt " +
            "FROM wishlist WHERE user_id = #{userId} AND pet_id = #{petId}")
    Wishlist findByUserAndPet(@Param("userId") Long userId, @Param("petId") Long petId);

    @Select("SELECT id, user_id AS userId, pet_id AS petId, created_at AS createdAt " +
            "FROM wishlist WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Wishlist> findByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM wishlist WHERE pet_id = #{petId}")
    int countByPetId(Long petId);

    @Insert("INSERT INTO wishlist (user_id, pet_id, created_at) VALUES (#{userId}, #{petId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Wishlist wishlist);

    @Delete("DELETE FROM wishlist WHERE user_id = #{userId} AND pet_id = #{petId}")
    void delete(@Param("userId") Long userId, @Param("petId") Long petId);
}

