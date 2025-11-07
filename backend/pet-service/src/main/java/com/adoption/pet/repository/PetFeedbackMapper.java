package com.adoption.pet.repository;

import com.adoption.pet.model.PetFeedback;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PetFeedbackMapper {

    @Select("SELECT id, pet_id AS petId, user_id AS userId, content, media_urls AS mediaUrls, created_at AS createdAt " +
            "FROM pet_feedback WHERE pet_id = #{petId} ORDER BY created_at DESC")
    List<PetFeedback> findByPetId(Long petId);

    @Select("SELECT id, pet_id AS petId, user_id AS userId, content, media_urls AS mediaUrls, created_at AS createdAt " +
            "FROM pet_feedback WHERE id = #{id}")
    PetFeedback findById(Long id);

    @Insert("INSERT INTO pet_feedback (pet_id, user_id, content, media_urls, created_at) " +
            "VALUES (#{petId}, #{userId}, #{content}, #{mediaUrls}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PetFeedback feedback);

    @Select({
            "<script>",
            "SELECT f.id, f.pet_id AS petId, f.user_id AS userId, f.content, f.media_urls AS mediaUrls, f.created_at AS createdAt ",
            "FROM pet_feedback f JOIN pet p ON f.pet_id = p.id ",
            "WHERE 1=1 ",
            "<if test=\"type != null and type != ''\">",
            "   AND p.type = #{type} ",
            "</if>",
            "ORDER BY f.created_at DESC",
            "</script>"
    })
    List<PetFeedback> findByPetType(@Param("type") String type);

}

