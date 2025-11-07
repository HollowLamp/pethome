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

    /**
     * 插入机构记录
     * @param org Organization 实体（使用自增主键，插入后回填 id）
     */
    @Insert("INSERT INTO org (name, license_url, address, contact_name, contact_phone, status, created_by, created_at, updated_at) " +
            "VALUES (#{name}, #{licenseUrl}, #{address}, #{contactName}, #{contactPhone}, #{status}, #{createdBy}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Organization org);

    /**
     * 根据主键查询机构
     */
    @Select("SELECT * FROM org WHERE id = #{id}")
    Organization findById(Long id);

    /**
     * 更新机构全量字段（演示用，生产建议限定更新字段）
     */
    @Update("UPDATE org SET name=#{name}, license_url=#{licenseUrl}, address=#{address}, contact_name=#{contactName}, contact_phone=#{contactPhone}, status=#{status}, updated_at=#{updatedAt} WHERE id=#{id}")
    void update(Organization org);

    /**
     * 更新机构状态与更新时间
     */
    @Update("UPDATE org SET status=#{status}, updated_at=#{updatedAt} WHERE id=#{id}")
    void updateStatus(@Param("id") Long id, @Param("status") String status, @Param("updatedAt") java.time.LocalDateTime updatedAt);
}

