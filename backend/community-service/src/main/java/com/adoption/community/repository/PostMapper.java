package com.adoption.community.repository;

import com.adoption.community.model.Post;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 帖子数据访问层（Mapper）
 *
 * 作用：提供帖子相关的数据库操作接口
 *
 * 使用MyBatis注解方式编写SQL，主要功能包括：
 * - 查询帖子（按ID、作者、类型、状态等）
 * - 插入新帖子
 * - 更新帖子状态和推荐状态
 * - 删除帖子
 * - 统计帖子数量
 */
@Mapper
public interface PostMapper {

    /**
     * 根据ID查询帖子
     *
     * @param id 帖子ID
     * @return 帖子对象，如果不存在返回null
     */
    @Select("SELECT id, author_id AS authorId, type, title, content, media_urls AS mediaUrls, " +
            "bind_pet_id AS bindPetId, ai_summary AS aiSummary, ai_flagged AS aiFlagged, " +
            "status, recommend, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM post WHERE id = #{id}")
    Post findById(Long id);

    /**
     * 查询帖子列表（支持类型筛选、排序、分页）
     *
     * 功能说明：
     * - 只查询状态为PUBLISHED（已发布）的帖子
     * - 支持按类型（type）筛选：PET_PUBLISH、DAILY、GUIDE
     * - 支持按推荐状态（recommend）筛选：true-只查推荐，false-只查非推荐，null-全部
     * - 支持排序方式：
     *   * latest或null：按创建时间倒序（最新优先）
     *   * popular：按点赞数倒序，相同点赞数按时间倒序（最热优先）
     *
     * @param type 帖子类型（可选，null表示不筛选）
     * @param sort 排序方式（latest-最新，popular-最热）
     * @param recommend 是否推荐（可选，null表示不筛选）
     * @param offset 偏移量（用于分页）
     * @param limit 每页数量
     * @return 帖子列表
     */
    @Select({
        "<script>",
        "SELECT id, author_id AS authorId, type, title, content, media_urls AS mediaUrls, ",
        "bind_pet_id AS bindPetId, ai_summary AS aiSummary, ai_flagged AS aiFlagged, ",
        "status, recommend, created_at AS createdAt, updated_at AS updatedAt ",
        "FROM post WHERE status = 'PUBLISHED'",
        "<if test='type != null and type != \"\"'> AND type = #{type} </if>",
        "<if test='recommend != null'> AND recommend = #{recommend} </if>",
        "<choose>",
        "  <when test='sort == \"latest\" or sort == null'> ORDER BY created_at DESC </when>",
        "  <when test='sort == \"popular\"'> ORDER BY (SELECT COUNT(*) FROM reaction WHERE reaction.post_id = post.id) DESC, created_at DESC </when>",
        "  <otherwise> ORDER BY created_at DESC </otherwise>",
        "</choose>",
        "LIMIT #{limit} OFFSET #{offset}",
        "</script>"
    })
    List<Post> findAll(@Param("type") String type,
                       @Param("sort") String sort,
                       @Param("recommend") Boolean recommend,
                       @Param("offset") int offset,
                       @Param("limit") int limit);

    /**
     * 统计帖子总数（用于分页计算总页数）
     *
     * @param type 帖子类型（可选，null表示不筛选）
     * @param recommend 是否推荐（可选，null表示不筛选）
     * @return 符合条件的帖子总数
     */
    @Select({
        "<script>",
        "SELECT COUNT(*) FROM post WHERE status = 'PUBLISHED'",
        "<if test='type != null and type != \"\"'> AND type = #{type} </if>",
        "<if test='recommend != null'> AND recommend = #{recommend} </if>",
        "</script>"
    })
    int countAll(@Param("type") String type, @Param("recommend") Boolean recommend);

    /**
     * 根据作者ID查询帖子列表（用于"我的帖子"功能）
     *
     * @param authorId 作者用户ID
     * @param offset 偏移量（用于分页）
     * @param limit 每页数量
     * @return 帖子列表（按创建时间倒序）
     */
    @Select("SELECT id, author_id AS authorId, type, title, content, media_urls AS mediaUrls, " +
            "bind_pet_id AS bindPetId, ai_summary AS aiSummary, ai_flagged AS aiFlagged, " +
            "status, recommend, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM post WHERE author_id = #{authorId} ORDER BY created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<Post> findByAuthorId(@Param("authorId") Long authorId,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    /**
     * 统计指定作者的帖子总数
     *
     * @param authorId 作者用户ID
     * @return 帖子总数
     */
    @Select("SELECT COUNT(*) FROM post WHERE author_id = #{authorId}")
    int countByAuthorId(Long authorId);

    /**
     * 查询AI标记的违规帖子（用于客服审核）
     *
     * @param offset 偏移量（用于分页）
     * @param limit 每页数量
     * @return AI标记的违规帖子列表（按创建时间倒序）
     */
    @Select("SELECT id, author_id AS authorId, type, title, content, media_urls AS mediaUrls, " +
            "bind_pet_id AS bindPetId, ai_summary AS aiSummary, ai_flagged AS aiFlagged, " +
            "status, recommend, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM post WHERE ai_flagged = TRUE ORDER BY created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<Post> findFlaggedPosts(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计AI标记的违规帖子总数
     *
     * @return 违规帖子总数
     */
    @Select("SELECT COUNT(*) FROM post WHERE ai_flagged = TRUE")
    int countFlaggedPosts();

    /**
     * 插入新帖子
     *
     * 注意：使用@Options注解自动获取数据库生成的主键ID并设置到post对象的id属性
     *
     * @param post 帖子对象（id会被自动设置）
     */
    @Insert("INSERT INTO post (author_id, type, title, content, media_urls, bind_pet_id, " +
            "ai_summary, ai_flagged, status, recommend, created_at, updated_at) " +
            "VALUES (#{authorId}, #{type}, #{title}, #{content}, #{mediaUrls}, #{bindPetId}, " +
            "#{aiSummary}, #{aiFlagged}, #{status}, #{recommend}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Post post);

    /**
     * 更新帖子状态（用于客服审核）
     *
     * 注意：处理完帖子后，清除AI标记（ai_flagged = false），因为人工审核已完成
     *
     * @param id 帖子ID
     * @param status 新状态（PUBLISHED、FLAGGED、REMOVED）
     */
    @Update("UPDATE post SET status = #{status}, ai_flagged = FALSE, updated_at = NOW() WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 仅更新帖子状态（不清除AI标记）
     *
     * 用于AI服务标记违规时，只更新status，保留ai_flagged标记
     *
     * @param id 帖子ID
     * @param status 新状态（PUBLISHED、FLAGGED、REMOVED）
     */
    @Update("UPDATE post SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    void updateStatusOnly(@Param("id") Long id, @Param("status") String status);

    /**
     * 更新帖子推荐状态（用于管理员推荐/取消推荐）
     *
     * @param id 帖子ID
     * @param recommend 是否推荐（true-推荐，false-取消推荐）
     */
    @Update("UPDATE post SET recommend = #{recommend}, updated_at = NOW() WHERE id = #{id}")
    void updateRecommend(@Param("id") Long id, @Param("recommend") Boolean recommend);

    /**
     * 更新帖子 AI 摘要
     *
     * @param id 帖子ID
     * @param aiSummary AI 生成的摘要
     */
    @Update("UPDATE post SET ai_summary = #{aiSummary}, updated_at = NOW() WHERE id = #{id}")
    void updateAiSummary(@Param("id") Long id, @Param("aiSummary") String aiSummary);

    /**
     * 更新帖子 AI 标记状态
     *
     * @param id 帖子ID
     * @param aiFlagged AI 是否标记为违规
     */
    @Update("UPDATE post SET ai_flagged = #{aiFlagged}, updated_at = NOW() WHERE id = #{id}")
    void updateAiFlagged(@Param("id") Long id, @Param("aiFlagged") Boolean aiFlagged);

    /**
     * 删除帖子（只能删除自己的帖子）
     *
     * 注意：同时验证id和authorId，确保用户只能删除自己的帖子
     *
     * @param id 帖子ID
     * @param authorId 作者ID
     * @return 删除的行数（0表示删除失败，1表示删除成功）
     */
    @Delete("DELETE FROM post WHERE id = #{id} AND author_id = #{authorId}")
    int deleteByIdAndAuthorId(@Param("id") Long id, @Param("authorId") Long authorId);
}

