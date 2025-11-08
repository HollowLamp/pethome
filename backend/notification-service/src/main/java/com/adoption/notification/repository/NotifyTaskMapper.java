package com.adoption.notification.repository;

import com.adoption.notification.model.NotifyTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface NotifyTaskMapper {

    @Insert("INSERT INTO notify_task (user_id, channel, template_code, payload, status, retry_count, created_at) " +
            "VALUES (#{userId}, #{channel}, #{templateCode}, #{payload}, #{status}, #{retryCount}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NotifyTask task);

    @Select("SELECT id, user_id AS userId, channel, template_code AS templateCode, payload, status, retry_count AS retryCount, created_at AS createdAt " +
            "FROM notify_task WHERE user_id = #{userId} AND status = 'SENT' ORDER BY created_at DESC")
    List<NotifyTask> selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE notify_task SET status = #{status}, retry_count = #{retryCount} WHERE id = #{id}")
    int updateStatus(NotifyTask task);

    @Select("SELECT id, user_id AS userId, channel, template_code AS templateCode, payload, status, retry_count AS retryCount, created_at AS createdAt " +
            "FROM notify_task WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT #{limit}")
    List<NotifyTask> selectPendingTasks(@Param("limit") int limit);
}

