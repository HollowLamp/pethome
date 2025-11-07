package com.adoption.pet.repository;

import com.adoption.pet.model.Pet;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PetMapper {

    @Select("SELECT id, org_id AS orgId, name, type, breed, gender, age, color, size, status, " +
            "cover_url AS coverUrl, description, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM pet WHERE id = #{id}")
    Pet findById(Long id);

    @Select({
        "<script>",
        "SELECT id, org_id AS orgId, name, type, breed, gender, age, color, size, status, ",
        "cover_url AS coverUrl, description, created_at AS createdAt, updated_at AS updatedAt ",
        "FROM pet WHERE 1=1",
        "<if test='type != null'> AND type = #{type} </if>",
        "<if test='status != null'> AND status = #{status} </if>",
        "<if test='orgId != null'> AND org_id = #{orgId} </if>",
        "ORDER BY created_at DESC",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<Pet> findAll(@Param("type") String type,
                      @Param("status") String status,
                      @Param("orgId") Long orgId,
                      @Param("offset") int offset,
                      @Param("limit") int limit);

    @Select({
        "<script>",
        "SELECT COUNT(*) FROM pet WHERE 1=1",
        "<if test='type != null'> AND type = #{type} </if>",
        "<if test='status != null'> AND status = #{status} </if>",
        "<if test='orgId != null'> AND org_id = #{orgId} </if>",
        "</script>"
    })
    int countAll(@Param("type") String type,
                 @Param("status") String status,
                 @Param("orgId") Long orgId);

    @Insert("INSERT INTO pet (org_id, name, type, breed, gender, age, color, size, status, cover_url, description, created_at, updated_at) " +
            "VALUES (#{orgId}, #{name}, #{type}, #{breed}, #{gender}, #{age}, #{color}, #{size}, #{status}, #{coverUrl}, #{description}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Pet pet);

    @Update({
        "<script>",
        "UPDATE pet SET",
        "<if test='name != null'> name = #{name}, </if>",
        "<if test='type != null'> type = #{type}, </if>",
        "<if test='breed != null'> breed = #{breed}, </if>",
        "<if test='gender != null'> gender = #{gender}, </if>",
        "<if test='age != null'> age = #{age}, </if>",
        "<if test='color != null'> color = #{color}, </if>",
        "<if test='size != null'> size = #{size}, </if>",
        "<if test='coverUrl != null'> cover_url = #{coverUrl}, </if>",
        "<if test='description != null'> description = #{description}, </if>",
        " updated_at = NOW()",
        " WHERE id = #{id}",
        "</script>"
    })
    void update(Pet pet);

    @Update("UPDATE pet SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") String status);
}

