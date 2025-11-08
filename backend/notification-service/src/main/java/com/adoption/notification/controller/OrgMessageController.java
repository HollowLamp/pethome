package com.adoption.notification.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.common.util.UserContext;
import com.adoption.notification.model.InboxMessage;
import com.adoption.notification.model.NotifyTask;
import com.adoption.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * B端用户消息接口
 */
@RestController
@RequestMapping("notification/org/messages")
public class OrgMessageController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserContext userContext;

    /**
     * B端用户获取系统通知
     * GET /notification/org/messages/system
     */
    @GetMapping("/system")
    public ApiResponse<List<InboxMessage>> getOrgSystemMessages() {
        Long userId = userContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        // B端和C端使用相同的逻辑，但可以通过角色判断来区分
        List<InboxMessage> messages = notificationService.getSystemMessages(userId);
        return ApiResponse.success(messages);
    }

    /**
     * 标记单条消息为已读
     * PUT /notification/org/messages/{id}/read
     */
    @PutMapping("/{id}/read")
    public ApiResponse<String> markMessageAsRead(@PathVariable("id") Long messageId) {
        Long userId = userContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        notificationService.markMessageAsRead(messageId);
        return ApiResponse.success("消息已标记为已读");
    }

    /**
     * 标记所有消息为已读
     * PUT /notification/org/messages/read-all
     */
    @PutMapping("/read-all")
    public ApiResponse<String> markAllMessagesAsRead() {
        Long userId = userContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        notificationService.markAllMessagesAsRead(userId);
        return ApiResponse.success("所有消息已标记为已读");
    }
}

