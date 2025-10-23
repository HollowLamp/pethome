package com.adoption.auth.repository;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserRoleMapper {

    @Select("SELECT role FROM user_role WHERE user_id = #{userId}")
    List<String> findRolesByUserId(Long userId);

    @Insert("INSERT INTO user_role (user_id, role, created_at) " +
            "VALUES (#{userId}, #{role}, NOW())")
    void insertUserRole(@Param("userId") Long userId, @Param("role") String role);

    @Delete("DELETE FROM user_role WHERE user_id = #{userId} AND role = #{role}")
    void deleteUserRole(@Param("userId") Long userId, @Param("role") String role);
}
