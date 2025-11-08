package com.adoption.community.repository;

import com.adoption.community.model.Report;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 举报数据访问层（Mapper）
 * 
 * 作用：提供举报相关的数据库操作接口
 * 
 * 主要功能包括：
 * - 查询举报记录（按ID、状态、举报人等）
 * - 插入新举报
 * - 更新举报状态（处理举报）
 * - 统计举报数量
 */
@Mapper
public interface ReportMapper {

    /**
     * 根据ID查询举报记录
     * 
     * @param id 举报记录ID
     * @return 举报对象，如果不存在返回null
     */
    @Select("SELECT id, post_id AS postId, comment_id AS commentId, reporter_id AS reporterId, " +
            "reason, status, reviewed_by AS reviewedBy, reviewed_at AS reviewedAt, created_at AS createdAt " +
            "FROM report WHERE id = #{id}")
    Report findById(Long id);

    /**
     * 查询举报列表（支持按状态筛选、分页）
     * 
     * 用于客服查看待处理或已处理的举报
     * 
     * @param status 处理状态（可选，PENDING-待处理，REVIEWED-已处理，null-全部）
     * @param offset 偏移量（用于分页）
     * @param limit 每页数量
     * @return 举报列表（按创建时间倒序）
     */
    @Select({
        "<script>",
        "SELECT id, post_id AS postId, comment_id AS commentId, reporter_id AS reporterId, ",
        "reason, status, reviewed_by AS reviewedBy, reviewed_at AS reviewedAt, created_at AS createdAt ",
        "FROM report WHERE 1=1",
        "<if test='status != null and status != \"\"'> AND status = #{status} </if>",
        "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<Report> findAll(@Param("status") String status,
                         @Param("offset") int offset,
                         @Param("limit") int limit);

    /**
     * 统计举报总数（用于分页计算总页数）
     * 
     * @param status 处理状态（可选，null表示不筛选）
     * @return 符合条件的举报总数
     */
    @Select({
        "<script>",
        "SELECT COUNT(*) FROM report WHERE 1=1",
        "<if test='status != null and status != \"\"'> AND status = #{status} </if>",
        "</script>"
    })
    int countAll(@Param("status") String status);

    /**
     * 查询用户对指定帖子的举报记录（用于防止重复举报）
     * 
     * @param reporterId 举报人ID
     * @param postId 帖子ID
     * @return 举报记录，如果不存在返回null
     */
    @Select("SELECT id, post_id AS postId, comment_id AS commentId, reporter_id AS reporterId, " +
            "reason, status, reviewed_by AS reviewedBy, reviewed_at AS reviewedAt, created_at AS createdAt " +
            "FROM report WHERE reporter_id = #{reporterId} AND post_id = #{postId}")
    Report findByReporterIdAndPostId(@Param("reporterId") Long reporterId, @Param("postId") Long postId);

    /**
     * 查询用户对指定评论的举报记录（用于防止重复举报）
     * 
     * @param reporterId 举报人ID
     * @param commentId 评论ID
     * @return 举报记录，如果不存在返回null
     */
    @Select("SELECT id, post_id AS postId, comment_id AS commentId, reporter_id AS reporterId, " +
            "reason, status, reviewed_by AS reviewedBy, reviewed_at AS reviewedAt, created_at AS createdAt " +
            "FROM report WHERE reporter_id = #{reporterId} AND comment_id = #{commentId}")
    Report findByReporterIdAndCommentId(@Param("reporterId") Long reporterId, @Param("commentId") Long commentId);

    /**
     * 插入新举报记录
     * 
     * 注意：使用@Options注解自动获取数据库生成的主键ID并设置到report对象的id属性
     * 
     * @param report 举报对象（id会被自动设置）
     */
    @Insert("INSERT INTO report (post_id, comment_id, reporter_id, reason, status, created_at) " +
            "VALUES (#{postId}, #{commentId}, #{reporterId}, #{reason}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Report report);

    /**
     * 更新举报状态（处理举报）
     * 
     * 用于客服审核举报，将状态从PENDING改为REVIEWED，并记录处理人
     * 
     * @param id 举报记录ID
     * @param status 新状态（REVIEWED）
     * @param reviewedBy 处理人用户ID（客服人员）
     */
    @Update("UPDATE report SET status = #{status}, reviewed_by = #{reviewedBy}, reviewed_at = NOW() " +
            "WHERE id = #{id}")
    void updateStatus(@Param("id") Long id,
                      @Param("status") String status,
                      @Param("reviewedBy") Long reviewedBy);
}

