package com.adoption.community.repository;

import com.adoption.community.model.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 评论数据访问层（Mapper）
 * 
 * 作用：提供评论相关的数据库操作接口
 * 
 * 主要功能包括：
 * - 查询评论（按ID、帖子ID）
 * - 插入新评论
 * - 删除评论
 * - 统计评论数量
 */
@Mapper
public interface CommentMapper {

    /**
     * 根据ID查询评论
     * 
     * @param id 评论ID
     * @return 评论对象，如果不存在返回null
     */
    @Select("SELECT id, post_id AS postId, author_id AS authorId, content, status, created_at AS createdAt " +
            "FROM comment WHERE id = #{id}")
    Comment findById(Long id);

    /**
     * 根据帖子ID查询评论列表（只查询可见的评论）
     * 
     * 注意：按创建时间正序排列，最早发布的评论在前
     * 
     * @param postId 帖子ID
     * @param offset 偏移量（用于分页）
     * @param limit 每页数量
     * @return 评论列表（按创建时间正序）
     */
    @Select("SELECT id, post_id AS postId, author_id AS authorId, content, status, created_at AS createdAt " +
            "FROM comment WHERE post_id = #{postId} AND status = 'VISIBLE' " +
            "ORDER BY created_at ASC LIMIT #{limit} OFFSET #{offset}")
    List<Comment> findByPostId(@Param("postId") Long postId,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    /**
     * 统计指定帖子的可见评论总数
     * 
     * @param postId 帖子ID
     * @return 评论总数
     */
    @Select("SELECT COUNT(*) FROM comment WHERE post_id = #{postId} AND status = 'VISIBLE'")
    int countByPostId(Long postId);

    /**
     * 插入新评论
     * 
     * 注意：使用@Options注解自动获取数据库生成的主键ID并设置到comment对象的id属性
     * 
     * @param comment 评论对象（id会被自动设置）
     */
    @Insert("INSERT INTO comment (post_id, author_id, content, status, created_at) " +
            "VALUES (#{postId}, #{authorId}, #{content}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Comment comment);

    /**
     * 删除评论（只能删除自己的评论）
     * 
     * 注意：同时验证id和authorId，确保用户只能删除自己的评论
     * 
     * @param id 评论ID
     * @param authorId 作者ID
     * @return 删除的行数（0表示删除失败，1表示删除成功）
     */
    @Delete("DELETE FROM comment WHERE id = #{id} AND author_id = #{authorId}")
    int deleteByIdAndAuthorId(@Param("id") Long id, @Param("authorId") Long authorId);
}

