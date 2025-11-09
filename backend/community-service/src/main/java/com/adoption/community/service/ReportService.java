package com.adoption.community.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.community.feign.AuthServiceClient;
import com.adoption.community.model.Comment;
import com.adoption.community.model.Post;
import com.adoption.community.model.Report;
import com.adoption.community.repository.CommentMapper;
import com.adoption.community.repository.PostMapper;
import com.adoption.community.repository.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 举报服务层
 *
 * 作用：处理举报相关的业务逻辑
 *
 * 主要功能：
 * - 举报帖子
 * - 举报评论
 * - 获取举报列表（分页）
 * - 处理举报
 *
 * 注意：举报提交后和处理后，可以考虑发送通知给相关用户（通过NotificationMessageService）
 */
@Service
public class ReportService {
    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    @Autowired
    private final NotificationMessageService notificationMessageService;
    @Autowired
    private final AuthServiceClient authServiceClient;

    public ReportService(ReportMapper reportMapper, PostMapper postMapper, CommentMapper commentMapper, NotificationMessageService notificationMessageService, AuthServiceClient authServiceClient) {
        this.reportMapper = reportMapper;
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.notificationMessageService = notificationMessageService;
        this.authServiceClient = authServiceClient;
    }

    /**
     * 举报帖子
     *
     * 功能说明：
     * - 验证帖子存在
     * - 检查用户是否已举报过该帖子（防止重复举报）
     * - 验证举报原因不能为空
     * - 自动设置帖子ID、举报人ID、默认状态为PENDING
     * - 插入数据库后返回包含ID的举报对象
     * - 发送通知给客服人员
     *
     * @param postId 被举报的帖子ID
     * @param report 举报对象（需要包含reason字段）
     * @param reporterId 举报人用户ID（从UserContext获取）
     * @return 创建成功的举报对象（包含自动生成的ID）
     */
    public ApiResponse<Report> reportPost(Long postId, Report report, Long reporterId) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            return ApiResponse.error(404, "帖子不存在");
        }

        // 检查是否已经举报过
        Report existing = reportMapper.findByReporterIdAndPostId(reporterId, postId);
        if (existing != null) {
            return ApiResponse.error(400, "您已经举报过该帖子");
        }

        if (report.getReason() == null || report.getReason().trim().isEmpty()) {
            return ApiResponse.error(400, "举报原因不能为空");
        }

        report.setPostId(postId);
        report.setReporterId(reporterId);
        if (report.getStatus() == null) {
            report.setStatus("PENDING");
        }

        reportMapper.insert(report);

        // 发送通知给客服人员
        try {
            // 获取举报者用户名
            String reporterName = "用户";
            try {
                ApiResponse<Map<String, Object>> userResponse = authServiceClient.getUserById(reporterId);
                if (userResponse != null && userResponse.getCode() == 200 && userResponse.getData() != null) {
                    Map<String, Object> userData = userResponse.getData();
                    Object usernameObj = userData.get("username");
                    if (usernameObj != null) {
                        reporterName = usernameObj.toString();
                    }
                }
            } catch (Exception e) {
                // 如果获取用户名失败，使用默认值
                System.err.println("获取用户名失败: " + e.getMessage());
            }

            List<Long> csUserIds = notificationMessageService.getUserIdsByRole("CS");
            for (Long csUserId : csUserIds) {
                notificationMessageService.sendSystemNotification(
                    csUserId,
                    "收到新的帖子举报",
                    String.format("%s举报了帖子《%s》，举报原因：%s", reporterName, post.getTitle(), report.getReason())
                );
            }
        } catch (Exception e) {
            System.err.println("发送通知失败: " + e.getMessage());
        }

        return ApiResponse.success(report);
    }

    /**
     * 举报评论
     *
     * 功能说明：
     * - 验证评论存在
     * - 检查用户是否已举报过该评论（防止重复举报）
     * - 验证举报原因不能为空
     * - 自动设置评论ID、举报人ID、默认状态为PENDING
     * - 插入数据库后返回包含ID的举报对象
     * - 发送通知给客服人员
     *
     * @param commentId 被举报的评论ID
     * @param report 举报对象（需要包含reason字段）
     * @param reporterId 举报人用户ID（从UserContext获取）
     * @return 创建成功的举报对象（包含自动生成的ID）
     */
    public ApiResponse<Report> reportComment(Long commentId, Report report, Long reporterId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment == null) {
            return ApiResponse.error(404, "评论不存在");
        }

        // 检查是否已经举报过
        Report existing = reportMapper.findByReporterIdAndCommentId(reporterId, commentId);
        if (existing != null) {
            return ApiResponse.error(400, "您已经举报过该评论");
        }

        if (report.getReason() == null || report.getReason().trim().isEmpty()) {
            return ApiResponse.error(400, "举报原因不能为空");
        }

        report.setCommentId(commentId);
        report.setReporterId(reporterId);
        if (report.getStatus() == null) {
            report.setStatus("PENDING");
        }

        reportMapper.insert(report);

        // 发送通知给客服人员
        try {
            // 获取举报者用户名
            String reporterName = "用户";
            try {
                ApiResponse<Map<String, Object>> userResponse = authServiceClient.getUserById(reporterId);
                if (userResponse != null && userResponse.getCode() == 200 && userResponse.getData() != null) {
                    Map<String, Object> userData = userResponse.getData();
                    Object usernameObj = userData.get("username");
                    if (usernameObj != null) {
                        reporterName = usernameObj.toString();
                    }
                }
            } catch (Exception e) {
                // 如果获取用户名失败，使用默认值
                System.err.println("获取用户名失败: " + e.getMessage());
            }

            List<Long> csUserIds = notificationMessageService.getUserIdsByRole("CS");
            for (Long csUserId : csUserIds) {
                notificationMessageService.sendSystemNotification(
                    csUserId,
                    "收到新的评论举报",
                    String.format("%s举报了评论：%s，举报原因：%s", reporterName, comment.getContent().length() > 20 ? comment.getContent().substring(0, 20) + "..." : comment.getContent(), report.getReason())
                );
            }
        } catch (Exception e) {
            System.err.println("发送通知失败: " + e.getMessage());
        }

        return ApiResponse.success(report);
    }

    /**
     * 获取举报列表（客服审核功能）
     *
     * 功能说明：
     * - 支持按状态筛选：PENDING（待处理）、REVIEWED（已处理）、null（全部）
     * - 支持分页查询
     * - 按创建时间倒序排列（最新的在前）
     * - 自动填充帖子/评论详情、举报人姓名、被举报内容作者姓名等字段
     *
     * 权限要求：需要CS（客服）角色
     *
     * @param status 处理状态（可选，PENDING-待处理，REVIEWED-已处理，null-全部）
     * @param page 页码（从1开始，默认1）
     * @param pageSize 每页数量（默认10，最大100）
     * @return 包含举报列表、总数、页码等信息的响应
     */
    public ApiResponse<Map<String, Object>> getReports(String status, Integer page, Integer pageSize) {
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
        List<Report> reports = reportMapper.findAll(status, offset, pageSize);
        int total = reportMapper.countAll(status);

        // 填充举报详情信息
        for (Report report : reports) {
            // 填充举报人姓名
            if (report.getReporterId() != null) {
                String reporterName = getUserName(report.getReporterId());
                report.setReporterName(reporterName);
            }

            // 如果是举报帖子，填充帖子标题和被举报内容作者姓名
            if (report.getPostId() != null) {
                Post post = postMapper.findById(report.getPostId());
                if (post != null) {
                    report.setPostTitle(post.getTitle());
                    // 填充被举报帖子作者姓名
                    if (post.getAuthorId() != null) {
                        String authorName = getUserName(post.getAuthorId());
                        report.setTargetAuthorName(authorName);
                    }
                }
            }

            // 如果是举报评论，填充评论内容和被举报内容作者姓名
            if (report.getCommentId() != null) {
                Comment comment = commentMapper.findById(report.getCommentId());
                if (comment != null) {
                    report.setCommentContent(comment.getContent());
                    // 填充被举报评论作者姓名
                    if (comment.getAuthorId() != null) {
                        String authorName = getUserName(comment.getAuthorId());
                        report.setTargetAuthorName(authorName);
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", reports);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ApiResponse.success(result);
    }

    /**
     * 根据用户ID获取用户名（辅助方法）
     *
     * @param userId 用户ID
     * @return 用户名，如果查询失败返回"未知"
     */
    private String getUserName(Long userId) {
        if (userId == null) {
            return "未知";
        }
        try {
            ApiResponse<Map<String, Object>> userResponse = authServiceClient.getUserById(userId);
            if (userResponse != null && userResponse.getCode() == 200 && userResponse.getData() != null) {
                Map<String, Object> userData = userResponse.getData();
                Object usernameObj = userData.get("username");
                if (usernameObj != null) {
                    return usernameObj.toString();
                }
            }
        } catch (Exception e) {
            System.err.println("获取用户信息失败 (userId: " + userId + "): " + e.getMessage());
        }
        return "未知";
    }

    /**
     * 处理举报（客服审核功能）
     *
     * 功能说明：
     * - 验证举报记录存在且状态为PENDING（待处理）
     * - 将状态更新为REVIEWED（已处理）
     * - 记录处理人ID和处理时间
     * - 发送通知给举报人
     *
     * 权限要求：需要CS（客服）角色
     *
     * @param id 举报记录ID
     * @param status 新状态（必须为REVIEWED）
     * @param reviewedBy 处理人用户ID（客服人员）
     * @return 处理结果
     */
    public ApiResponse<String> handleReport(Long id, String status, Long reviewedBy) {
        Report report = reportMapper.findById(id);
        if (report == null) {
            return ApiResponse.error(404, "举报记录不存在");
        }

        if (!"PENDING".equals(report.getStatus())) {
            return ApiResponse.error(400, "该举报已处理");
        }

        if (!"REVIEWED".equals(status)) {
            return ApiResponse.error(400, "无效的状态值");
        }

        reportMapper.updateStatus(id, status, reviewedBy);

        // 发送通知给举报人
        try {
            notificationMessageService.sendSystemNotification(
                report.getReporterId(),
                "举报处理完成",
                String.format("您对%s的举报已处理完成", report.getPostId() != null ? "帖子" : "评论")
            );
        } catch (Exception e) {
            System.err.println("发送通知失败: " + e.getMessage());
        }

        return ApiResponse.success("处理成功");
    }
}