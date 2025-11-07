package com.adoption.auth.repository;

import com.adoption.auth.model.UserAccount;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT id, username, email, phone, password_hash AS passwordHash, status " +
            "FROM user_account WHERE username = #{username}")
    UserAccount findByUsername(String username);

    @Insert("INSERT INTO user_account (username, email, phone, password_hash, status, created_at, updated_at) " +
            "VALUES (#{username}, #{email}, #{phone}, #{passwordHash}, 'ACTIVE', NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UserAccount user);

    @Select("SELECT id, username, email, phone, status, created_at AS createdAt " +
            "FROM user_account " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<UserAccount> findAll(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM user_account")
    int countAll();
}
