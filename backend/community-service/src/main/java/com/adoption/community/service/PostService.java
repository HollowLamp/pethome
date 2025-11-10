package com.adoption.community.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.common.util.UserContext;
import com.adoption.community.feign.AuthServiceClient;
import com.adoption.community.model.Post;
import com.adoption.community.model.Reaction;
import com.adoption.community.repository.PostMapper;
import com.adoption.community.repository.ReactionMapper;
import com.adoption.community.repository.CommentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 帖子服务层
 *
 * 作用：处理帖子相关的业务逻辑
 *
 * 主要功能：
 * - 帖子列表查询（支持类型筛选、排序、分页）
 * - 帖子详情查询
 * - 发布帖子
 * - 删除帖子
 * - 我的帖子查询
 * - 客服审核功能（查看违规帖子、修改状态）
 * - 管理员推荐功能
 */
@Service
public class PostService {
    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostMapper postMapper;
    private final ReactionMapper reactionMapper;
    private final CommentMapper commentMapper;
    @Autowired
    private final NotificationMessageService notificationMessageService;
    @Autowired
    private final AuthServiceClient authServiceClient;
    @Autowired
    private final AiAnalysisMessageService aiAnalysisMessageService;
    @Autowired
    private UserContext userContext;

    public PostService(PostMapper postMapper,
                      ReactionMapper reactionMapper,
                      CommentMapper commentMapper,
                      NotificationMessageService notificationMessageService,
                      AuthServiceClient authServiceClient,
                      AiAnalysisMessageService aiAnalysisMessageService) {
        this.postMapper = postMapper;
        this.reactionMapper = reactionMapper;
        this.commentMapper = commentMapper;
        this.notificationMessageService = notificationMessageService;
        this.authServiceClient = authServiceClient;
        this.aiAnalysisMessageService = aiAnalysisMessageService;
    }

    /**
     * 获取帖子列表（支持类型筛选、排序、分页）
     *
     * 功能说明：
     * - 只返回状态为PUBLISHED（已发布）的帖子
     * - 支持按类型筛选：PET_PUBLISH、DAILY、GUIDE
     * - 支持排序：latest（最新）、popular（最热）
     * - 支持分页查询
     *
     * @param type 帖子类型（可选，null表示不筛选）
     * @param sort 排序方式（latest-最新，popular-最热，null-默认最新）
     * @param page 页码（从1开始，默认1）
     * @param pageSize 每页数量（默认10，最大100）
     * @return 包含帖子列表、总数、页码等信息的响应
     */
    public ApiResponse<Map<String, Object>> getPostList(String type, String sort, Integer page, Integer pageSize) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }

        int offset = (page - 1) * pageSize;
        List<Post> posts = postMapper.findAll(type, sort, null, offset, pageSize);
        int total = postMapper.countAll(type, null);

        // 获取当前用户ID（如果未登录则为null）
        Long currentUserId = userContext.getCurrentUserId();

        // 为每个帖子统计点赞数、评论数和是否已点赞，并填充作者信息
        for (Post post : posts) {
            // 统计点赞数
            int likeCount = reactionMapper.countByPostId(post.getId());
            post.setLikeCount(likeCount);

            // 统计评论数
            int commentCount = commentMapper.countByPostId(post.getId());
            post.setCommentCount(commentCount);

            // 查询当前用户是否已点赞（如果已登录）
            if (currentUserId != null) {
                Reaction reaction = reactionMapper.findByUserIdAndPostId(currentUserId, post.getId(), "LIKE");
                post.setIsLiked(reaction != null);
            } else {
                post.setIsLiked(false);
            }

            // 填充作者信息
            if (post.getAuthorId() != null) {
                try {
                    ApiResponse<Map<String, Object>> userResponse = authServiceClient.getUserById(post.getAuthorId());
                    if (userResponse != null && userResponse.getCode() == 200 && userResponse.getData() != null) {
                        Map<String, Object> userData = userResponse.getData();
                        Object usernameObj = userData.get("username");
                        if (usernameObj != null) {
                            post.setAuthorName(usernameObj.toString());
                        }
                        Object avatarUrlObj = userData.get("avatarUrl");
                        if (avatarUrlObj != null) {
                            post.setAuthorAvatarUrl(avatarUrlObj.toString());
                        }
                    }
                } catch (Exception e) {
                    // 如果获取用户信息失败，不影响主流程，只记录日志
                    System.err.println("获取帖子作者信息失败，authorId: " + post.getAuthorId() + ", error: " + e.getMessage());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", posts);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ApiResponse.success(result);
    }

    /**
     * 获取帖子详情
     *
     * 注意：只返回状态为PUBLISHED的帖子，其他状态的帖子返回404
     *
     * @param id 帖子ID
     * @return 帖子详情，如果不存在或未发布则返回错误
     */
    public ApiResponse<Post> getPostById(Long id) {
        Post post = postMapper.findById(id);
        if (post == null) {
            return ApiResponse.error(404, "帖子不存在");
        }
        if (!"PUBLISHED".equals(post.getStatus())) {
            return ApiResponse.error(404, "帖子不存在或已被删除");
        }

        // 获取当前用户ID（如果未登录则为null）
        Long currentUserId = userContext.getCurrentUserId();

        // 统计点赞数
        int likeCount = reactionMapper.countByPostId(post.getId());
        post.setLikeCount(likeCount);

        // 统计评论数
        int commentCount = commentMapper.countByPostId(post.getId());
        post.setCommentCount(commentCount);

        // 查询当前用户是否已点赞（如果已登录）
        if (currentUserId != null) {
            Reaction reaction = reactionMapper.findByUserIdAndPostId(currentUserId, post.getId(), "LIKE");
            post.setIsLiked(reaction != null);
        } else {
            post.setIsLiked(false);
        }

        // 填充作者信息
        if (post.getAuthorId() != null) {
            try {
                ApiResponse<Map<String, Object>> userResponse = authServiceClient.getUserById(post.getAuthorId());
                if (userResponse != null && userResponse.getCode() == 200 && userResponse.getData() != null) {
                    Map<String, Object> userData = userResponse.getData();
                    Object usernameObj = userData.get("username");
                    if (usernameObj != null) {
                        post.setAuthorName(usernameObj.toString());
                    }
                    Object avatarUrlObj = userData.get("avatarUrl");
                    if (avatarUrlObj != null) {
                        post.setAuthorAvatarUrl(avatarUrlObj.toString());
                    }
                }
            } catch (Exception e) {
                // 如果获取用户信息失败，不影响主流程，只记录日志
                System.err.println("获取帖子作者信息失败，authorId: " + post.getAuthorId() + ", error: " + e.getMessage());
            }
        }

        return ApiResponse.success(post);
    }

    /**
     * 发布帖子
     *
     * 功能说明：
     * - 验证标题和类型不能为空
     * - 自动设置作者ID、默认状态为PUBLISHED
     * - 自动设置aiFlagged为false、recommend为false
     * - 插入数据库后返回包含ID的帖子对象
     * - 发送通知给关注该用户的粉丝
     *
     * 注意：媒体文件URL需要先通过文件上传接口获取，然后以JSON数组格式存储在mediaUrls字段
     *
     * @param post 帖子对象（需要包含title、type、content等字段）
     * @param authorId 作者用户ID（从UserContext获取）
     * @return 创建成功的帖子对象（包含自动生成的ID）
     */
    public ApiResponse<Post> createPost(Post post, Long authorId) {
        if (post.getTitle() == null || post.getTitle().trim().isEmpty()) {
            return ApiResponse.error(400, "标题不能为空");
        }
        if (post.getType() == null) {
            return ApiResponse.error(400, "帖子类型不能为空");
        }

        post.setAuthorId(authorId);
        if (post.getStatus() == null) {
            post.setStatus("PUBLISHED");
        }
        if (post.getAiFlagged() == null) {
            post.setAiFlagged(false);
        }
        if (post.getRecommend() == null) {
            post.setRecommend(false);
        }

        postMapper.insert(post);

        // 异步触发 AI 分析（不阻塞主流程）
        try {
            triggerAiAnalysis(post);
        } catch (Exception e) {
            // AI 分析失败不影响帖子发布
            log.warn("触发 AI 分析失败: postId={}, error={}", post.getId(), e.getMessage());
        }

        return ApiResponse.success(post);
    }

    /**
     * 触发 AI 分析（通过 RabbitMQ 异步处理）
     *
     * 根据帖子类型触发不同的 AI 分析：
     * 1. 所有帖子：违规检测（CONTENT_MOD）
     * 2. 养宠日常（DAILY）：提取宠物健康状态（STATE_EXTRACT）
     * 3. 养宠攻略（GUIDE）：生成内容总结（SUMMARY）
     */
    private void triggerAiAnalysis(Post post) {
        Long postId = post.getId();
        String type = post.getType();
        String title = post.getTitle();
        String content = post.getContent();
        Long bindPetId = post.getBindPetId();
        Long authorId = post.getAuthorId();

        // 1. 所有帖子都进行违规检测（异步）
        aiAnalysisMessageService.sendContentModerationRequest(postId, title, content, authorId);

        // 2. 养宠日常（DAILY）：提取宠物健康状态（异步）
        if ("DAILY".equals(type)) {
            aiAnalysisMessageService.sendStateExtractRequest(postId, content, bindPetId, authorId);
        }

        // 3. 养宠攻略（GUIDE）：生成内容总结（异步）
        if ("GUIDE".equals(type)) {
            aiAnalysisMessageService.sendSummaryRequest(postId, title, content, authorId);
        }
    }

    /**
     * 删除自己的帖子
     *
     * 注意：只能删除自己发布的帖子，通过同时验证id和authorId确保安全性
     *
     * @param id 帖子ID
     * @param authorId 作者用户ID（用于验证权限）
     * @return 删除结果
     */
    public ApiResponse<String> deletePost(Long id, Long authorId) {
        Post post = postMapper.findById(id);
        if (post == null) {
            return ApiResponse.error(404, "帖子不存在");
        }
        if (!post.getAuthorId().equals(authorId)) {
            return ApiResponse.error(403, "只能删除自己的帖子");
        }

        int deleted = postMapper.deleteByIdAndAuthorId(id, authorId);
        if (deleted > 0) {
            return ApiResponse.success("删除成功");
        } else {
            return ApiResponse.error(500, "删除失败");
        }
    }

    /**
     * 获取我发布的帖子（用于"我的帖子"功能）
     *
     * 功能说明：
     * - 查询指定用户发布的所有帖子（包括已发布、已删除等所有状态）
     * - 支持分页查询
     * - 按创建时间倒序排列
     *
     * @param authorId 作者用户ID
     * @param page 页码（从1开始，默认1）
     * @param pageSize 每页数量（默认10，最大100）
     * @return 包含帖子列表、总数、页码等信息的响应
     */
    public ApiResponse<Map<String, Object>> getMyPosts(Long authorId, Integer page, Integer pageSize) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }

        int offset = (page - 1) * pageSize;
        List<Post> posts = postMapper.findByAuthorId(authorId, offset, pageSize);
        int total = postMapper.countByAuthorId(authorId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", posts);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ApiResponse.success(result);
    }

    /**
     * 获取AI标记的违规帖子（客服审核功能）
     *
     * 功能说明：
     * - 查询所有被AI标记为违规的帖子（aiFlagged = true）
     * - 用于客服人员审核和处理违规内容
     * - 支持分页查询
     *
     * 权限要求：需要CS（客服）角色
     *
     * @param page 页码（从1开始，默认1）
     * @param pageSize 每页数量（默认10，最大100）
     * @return 包含违规帖子列表、总数、页码等信息的响应
     */
    public ApiResponse<Map<String, Object>> getFlaggedPosts(Integer page, Integer pageSize) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }

        int offset = (page - 1) * pageSize;
        List<Post> posts = postMapper.findFlaggedPosts(offset, pageSize);
        int total = postMapper.countFlaggedPosts();

        Map<String, Object> result = new HashMap<>();
        result.put("list", posts);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ApiResponse.success(result);
    }

    /**
     * 修改帖子状态（客服审核功能）
     *
     * 功能说明：
     * - 用于客服审核违规帖子后修改其状态
     * - 支持的状态：PUBLISHED（恢复发布）、FLAGGED（标记违规）、REMOVED（删除）
     * - 处理完帖子后，自动清除AI标记（aiFlagged = false），因为人工审核已完成
     *
     * 权限要求：需要CS（客服）角色
     *
     * @param id 帖子ID
     * @param status 新状态（PUBLISHED、FLAGGED、REMOVED）
     * @return 更新结果
     */
    public ApiResponse<String> updatePostStatus(Long id, String status) {
        Post post = postMapper.findById(id);
        if (post == null) {
            return ApiResponse.error(404, "帖子不存在");
        }

        if (!"PUBLISHED".equals(status) && !"FLAGGED".equals(status) && !"REMOVED".equals(status)) {
            return ApiResponse.error(400, "无效的状态值");
        }

        // 更新状态并清除AI标记（人工审核已完成）
        postMapper.updateStatus(id, status);
        return ApiResponse.success("状态更新成功");
    }

    /**
     * 推荐/取消推荐帖子（超级管理员功能）
     *
     * 功能说明：
     * - 用于管理员将优质帖子推荐到首页
     * - 如果recommend为null，则切换当前状态（推荐变不推荐，不推荐变推荐）
     * - 如果recommend有值，则设置为指定状态
     *
     * 权限要求：需要ADMIN（超级管理员）角色
     *
     * @param id 帖子ID
     * @param recommend 是否推荐（true-推荐，false-取消推荐，null-切换状态）
     * @return 操作结果
     */
    public ApiResponse<String> toggleRecommend(Long id, Boolean recommend) {
        Post post = postMapper.findById(id);
        if (post == null) {
            return ApiResponse.error(404, "帖子不存在");
        }

        if (recommend == null) {
            recommend = !post.getRecommend();
        }

        postMapper.updateRecommend(id, recommend);
        return ApiResponse.success(recommend ? "已推荐" : "已取消推荐");
    }

    /**
     * 更新帖子 AI 标记状态（AI 服务回调）
     */
    public void updatePostAiFlagged(Long postId, Boolean aiFlagged) {
        postMapper.updateAiFlagged(postId, aiFlagged);
    }

    /**
     * 仅更新帖子状态（不清除AI标记）
     *
     * 用于AI服务标记违规时，只更新status，保留ai_flagged标记
     */
    public void updatePostStatusOnly(Long postId, String status) {
        postMapper.updateStatusOnly(postId, status);
    }

    /**
     * 更新帖子 AI 总结（AI 服务回调）
     */
    public void updatePostAiSummary(Long postId, String aiSummary) {
        postMapper.updateAiSummary(postId, aiSummary);
    }
}

