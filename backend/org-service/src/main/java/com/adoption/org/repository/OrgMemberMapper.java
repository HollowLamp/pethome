package com.adoption.org.repository;

import com.adoption.org.entity.OrgMember;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 对应表 org_member 的 Mapper 接口
 * 负责机构成员表的增删查操作
 */
@Mapper
public interface OrgMemberMapper {

    /**
     * 插入新的机构成员
     * 说明：依赖表上的唯一键 (org_id, user_id) 防重复
     */
    @Insert("INSERT INTO org_member (org_id, user_id, created_at) " +
            "VALUES (#{orgId}, #{userId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(OrgMember member);

    /**
     * 根据 orgId 和 userId 查找成员
     */
    @Select("SELECT * FROM org_member WHERE org_id = #{orgId} AND user_id = #{userId}")
    OrgMember findByOrgIdAndUserId(@Param("orgId") Long orgId, @Param("userId") Long userId);

    /**
     * 删除某机构下的某成员
     */
    @Delete("DELETE FROM org_member WHERE org_id = #{orgId} AND user_id = #{userId}")
    void deleteByOrgIdAndUserId(@Param("orgId") Long orgId, @Param("userId") Long userId);

    /**
     * 查询某机构下的所有成员
     */
    @Select("SELECT * FROM org_member WHERE org_id = #{orgId}")
    List<OrgMember> findByOrgId(@Param("orgId") Long orgId);

    /**
     * 查询某用户加入的全部机构
     */
    @Select("SELECT * FROM org_member WHERE user_id = #{userId}")
    List<OrgMember> findByUserId(@Param("userId") Long userId);

    /**
     * 连表直接返回所有列（org.*）并附加成员关系必要列，避免字段名冲突做适当别名
     */
    @Select("SELECT " +
            " o.*, " +
            " m.id AS membership_id, m.created_at AS membership_created_at, m.user_id AS membership_user_id " +
            " FROM org_member m JOIN org o ON m.org_id = o.id WHERE m.user_id = #{userId}")
    List<Map<String, Object>> findMembershipsWithOrgByUserId(@Param("userId") Long userId);
}
