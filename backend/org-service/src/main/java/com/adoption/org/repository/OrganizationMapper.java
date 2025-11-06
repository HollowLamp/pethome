package com.adoption.org.repository;

import com.adoption.org.entity.Organization;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 对应 org 表的 MyBatis Mapper
 * DAO 层接口，用于执行增删改查数据库操作
 */
@Mapper
public interface OrganizationMapper {

    @Insert("INSERT INTO org (name, license_url, address, contact_name, contact_phone, status, created_by, created_at, updated_at) " +
            "VALUES (#{name}, #{licenseUrl}, #{address}, #{contactName}, #{contactPhone}, #{status}, #{createdBy}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Organization org);

    @Select("SELECT * FROM org WHERE id = #{id}")
    Organization findById(Long id);

    @Update("UPDATE org SET name=#{name}, license_url=#{licenseUrl}, address=#{address}, contact_name=#{contactName}, contact_phone=#{contactPhone}, status=#{status}, updated_at=#{updatedAt} WHERE id=#{id}")
    void update(Organization org);

    @Update("UPDATE org SET status=#{status}, updated_at=#{updatedAt} WHERE id=#{id}")
    void updateStatus(@Param("id") Long id, @Param("status") String status, @Param("updatedAt") java.time.LocalDateTime updatedAt);
}

