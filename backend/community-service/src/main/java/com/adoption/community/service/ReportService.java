package com.adoption.community.service;

import com.adoption.common.api.ApiResponse;
import com.adoption.community.model.Post;
import com.adoption.community.model.Comment;
import com.adoption.community.model.Report;
import com.adoption.community.repository.PostMapper;
import com.adoption.community.repository.CommentMapper;
import com.adoption.community.repository.ReportMapper;
import org.springframework.stereotype.Service;

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
 * - 获取举报列表（客服审核）
 * - 处理举报（客服审核）
 * 
 * 注意：
 * - 同一用户对同一帖子/评论只能举报一次
 * - 举报后可以发送通知给客服人员（通过NotificationMessageService）
 */
@Service
public class ReportService {
    private final ReportMapper reportMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    public ReportService(ReportMapper reportMapper, PostMapper postMapper, CommentMapper commentMapper) {
        this.reportMapper = reportMapper;
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
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
     * 
     * 扩展建议：可以在这里调用NotificationMessageService发送通知给客服人员
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
     * 
     * 扩展建议：可以在这里调用NotificationMessageService发送通知给客服人员
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
        return ApiResponse.success(report);
    }

    /**
     * 获取举报列表（客服审核功能）
     * 
     * 功能说明：
     * - 支持按状态筛选：PENDING（待处理）、REVIEWED（已处理）、null（全部）
     * - 支持分页查询
     * - 按创建时间倒序排列（最新的在前）
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

        Map<String, Object> result = new HashMap<>();
        result.put("list", reports);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ApiResponse.success(result);
    }

    /**
     * 处理举报（客服审核功能）
     * 
     * 功能说明：
     * - 验证举报记录存在且状态为PENDING（待处理）
     * - 将状态更新为REVIEWED（已处理）
     * - 记录处理人ID和处理时间
     * 
     * 权限要求：需要CS（客服）角色
     * 
     * 扩展建议：可以在这里调用NotificationMessageService发送通知给举报人
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
        return ApiResponse.success("处理成功");
    }
}

