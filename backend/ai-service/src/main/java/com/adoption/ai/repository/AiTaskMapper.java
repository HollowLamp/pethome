package com.adoption.ai.repository;

import com.adoption.ai.model.AiTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiTaskMapper {
    @Insert("INSERT INTO ai_task (type, source_id, source_type, status, result_json, confidence, created_at, updated_at) " +
            "VALUES (#{type}, #{sourceId}, #{sourceType}, #{status}, #{resultJson}, #{confidence}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AiTask task);

    @Select("SELECT * FROM ai_task WHERE id = #{id}")
    AiTask findById(Long id);

    @Select("SELECT * FROM ai_task WHERE source_type = #{sourceType} AND source_id = #{sourceId} AND type = #{type} ORDER BY created_at DESC LIMIT 1")
    AiTask findLatestBySource(String sourceType, Long sourceId, String type);

    @Select("SELECT * FROM ai_task WHERE source_type = #{sourceType} AND source_id = #{sourceId} ORDER BY created_at DESC")
    List<AiTask> findBySource(String sourceType, Long sourceId);

    @Update("UPDATE ai_task SET status = #{status}, result_json = #{resultJson}, confidence = #{confidence}, updated_at = #{updatedAt} WHERE id = #{id}")
    void update(AiTask task);

    @Update("UPDATE ai_task SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    void updateStatus(Long id, String status);
}

