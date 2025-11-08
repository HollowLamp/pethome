package com.adoption.notification.repository;

import com.adoption.notification.model.InboxMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InboxMessageMapper {

    @Insert("INSERT INTO inbox_message (to_user_id, title, body, is_read, created_at) " +
            "VALUES (#{toUserId}, #{title}, #{body}, #{isRead}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InboxMessage message);

    @Select("SELECT id, to_user_id AS toUserId, title, body, is_read AS isRead, created_at AS createdAt " +
            "FROM inbox_message WHERE to_user_id = #{userId} ORDER BY created_at DESC")
    List<InboxMessage> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT id, to_user_id AS toUserId, title, body, is_read AS isRead, created_at AS createdAt " +
            "FROM inbox_message WHERE to_user_id = #{userId} AND is_read = FALSE ORDER BY created_at DESC")
    List<InboxMessage> selectUnreadByUserId(@Param("userId") Long userId);

    @Update("UPDATE inbox_message SET is_read = TRUE WHERE id = #{id}")
    int markAsRead(@Param("id") Long id);

    @Update("UPDATE inbox_message SET is_read = TRUE WHERE to_user_id = #{userId}")
    int markAllAsRead(@Param("userId") Long userId);
}

