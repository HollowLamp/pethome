package com.adoption.notification.repository;

import com.adoption.notification.model.DirectMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DirectMessageMapper {

    @Insert("INSERT INTO direct_message (from_user_id, to_user_id, content, created_at) " +
            "VALUES (#{fromUserId}, #{toUserId}, #{content}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DirectMessage message);

    @Select("SELECT id, from_user_id AS fromUserId, to_user_id AS toUserId, content, created_at AS createdAt " +
            "FROM direct_message WHERE to_user_id = #{userId} OR from_user_id = #{userId} " +
            "ORDER BY created_at DESC")
    List<DirectMessage> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT id, from_user_id AS fromUserId, to_user_id AS toUserId, content, created_at AS createdAt " +
            "FROM direct_message WHERE (from_user_id = #{userId1} AND to_user_id = #{userId2}) " +
            "OR (from_user_id = #{userId2} AND to_user_id = #{userId1}) " +
            "ORDER BY created_at DESC")
    List<DirectMessage> selectConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Select("SELECT id, from_user_id AS fromUserId, to_user_id AS toUserId, content, created_at AS createdAt " +
            "FROM direct_message WHERE to_user_id = #{userId} ORDER BY created_at DESC")
    List<DirectMessage> selectReceivedByUserId(@Param("userId") Long userId);
}

