package com.adoption.pet.repository;

import com.adoption.pet.model.PetHealth;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PetHealthMapper {

    @Select("SELECT id, pet_id AS petId, weight, vaccine, note, updated_by AS updatedBy, updated_at AS updatedAt " +
            "FROM pet_health WHERE pet_id = #{petId} ORDER BY updated_at DESC LIMIT 1")
    PetHealth findLatestByPetId(Long petId);

    @Select("SELECT id, pet_id AS petId, weight, vaccine, note, updated_by AS updatedBy, updated_at AS updatedAt " +
            "FROM pet_health WHERE pet_id = #{petId} ORDER BY updated_at DESC")
    List<PetHealth> findAllByPetId(Long petId);

    @Insert("INSERT INTO pet_health (pet_id, weight, vaccine, note, updated_by, updated_at) " +
            "VALUES (#{petId}, #{weight}, #{vaccine}, #{note}, #{updatedBy}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PetHealth petHealth);

    @Update("UPDATE pet_health SET weight = #{weight}, vaccine = #{vaccine}, note = #{note}, updated_by = #{updatedBy}, updated_at = NOW() " +
            "WHERE pet_id = #{petId}")
    void updateByPetId(PetHealth petHealth);
}

