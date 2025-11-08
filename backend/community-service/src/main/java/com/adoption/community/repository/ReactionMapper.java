package com.adoption.community.repository;

import com.adoption.community.model.Reaction;
import org.apache.ibatis.annotations.*;

/**
 * 互动反应数据访问层（Mapper）
 * 
 * 作用：提供点赞等互动行为的数据库操作接口
 * 
 * 主要功能包括：
 * - 查询互动记录（检查用户是否已点赞）
 * - 插入互动记录（点赞）
 * - 删除互动记录（取消点赞）
 * - 统计点赞数量
 * 
 * 注意：通过唯一索引确保同一用户对同一帖子/评论只能有一条记录，实现幂等性
 */
@Mapper
public interface ReactionMapper {

    /**
     * 查询用户对帖子的互动记录（用于判断是否已点赞）
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     * @param type 互动类型（如"LIKE"）
     * @return 互动记录，如果不存在返回null
     */
    @Select("SELECT id, post_id AS postId, comment_id AS commentId, user_id AS userId, " +
            "type, created_at AS createdAt " +
            "FROM reaction WHERE user_id = #{userId} AND post_id = #{postId} AND type = #{type}")
    Reaction findByUserIdAndPostId(@Param("userId") Long userId,
                                   @Param("postId") Long postId,
                                   @Param("type") String type);

    /**
     * 查询用户对评论的互动记录（用于判断是否已点赞）
     * 
     * @param userId 用户ID
     * @param commentId 评论ID
     * @param type 互动类型（如"LIKE"）
     * @return 互动记录，如果不存在返回null
     */
    @Select("SELECT id, post_id AS postId, comment_id AS commentId, user_id AS userId, " +
            "type, created_at AS createdAt " +
            "FROM reaction WHERE user_id = #{userId} AND comment_id = #{commentId} AND type = #{type}")
    Reaction findByUserIdAndCommentId(@Param("userId") Long userId,
                                      @Param("commentId") Long commentId,
                                      @Param("type") String type);

    /**
     * 统计帖子的点赞数
     * 
     * @param postId 帖子ID
     * @return 点赞总数
     */
    @Select("SELECT COUNT(*) FROM reaction WHERE post_id = #{postId} AND type = 'LIKE'")
    int countByPostId(Long postId);

    /**
     * 统计评论的点赞数
     * 
     * @param commentId 评论ID
     * @return 点赞总数
     */
    @Select("SELECT COUNT(*) FROM reaction WHERE comment_id = #{commentId} AND type = 'LIKE'")
    int countByCommentId(Long commentId);

    /**
     * 插入互动记录（点赞）
     * 
     * 注意：使用@Options注解自动获取数据库生成的主键ID并设置到reaction对象的id属性
     * 
     * @param reaction 互动对象（id会被自动设置）
     */
    @Insert("INSERT INTO reaction (post_id, comment_id, user_id, type, created_at) " +
            "VALUES (#{postId}, #{commentId}, #{userId}, #{type}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Reaction reaction);

    /**
     * 删除用户对帖子的互动记录（取消点赞）
     * 
     * @param userId 用户ID
     * @param postId 帖子ID
     * @param type 互动类型（如"LIKE"）
     * @return 删除的行数（0表示删除失败，1表示删除成功）
     */
    @Delete("DELETE FROM reaction WHERE user_id = #{userId} AND post_id = #{postId} AND type = #{type}")
    int deleteByUserIdAndPostId(@Param("userId") Long userId,
                                 @Param("postId") Long postId,
                                 @Param("type") String type);

    /**
     * 删除用户对评论的互动记录（取消点赞）
     * 
     * @param userId 用户ID
     * @param commentId 评论ID
     * @param type 互动类型（如"LIKE"）
     * @return 删除的行数（0表示删除失败，1表示删除成功）
     */
    @Delete("DELETE FROM reaction WHERE user_id = #{userId} AND comment_id = #{commentId} AND type = #{type}")
    int deleteByUserIdAndCommentId(@Param("userId") Long userId,
                                   @Param("commentId") Long commentId,
                                   @Param("type") String type);
}

