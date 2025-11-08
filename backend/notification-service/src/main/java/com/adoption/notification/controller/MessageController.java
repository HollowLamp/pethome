package com.adoption.notification.controller;

import com.adoption.common.api.ApiResponse;
import com.adoption.common.util.UserContext;
import com.adoption.notification.model.DirectMessage;
import com.adoption.notification.model.InboxMessage;
import com.adoption.notification.model.NotifyTask;
import com.adoption.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("notification/me/messages")
public class MessageController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserContext userContext;

    /**
     * 用户获取系统通知
     * GET /notification/me/messages/system
     */
    @GetMapping("/system")
    public ApiResponse<List<InboxMessage>> getSystemMessages() {
        Long userId = userContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        List<InboxMessage> messages = notificationService.getSystemMessages(userId);
        return ApiResponse.success(messages);
    }

    /**
     * 用户获取私信消息
     * GET /notification/me/messages/direct
     */
    @GetMapping("/direct")
    public ApiResponse<List<DirectMessage>> getDirectMessages() {
        Long userId = userContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        List<DirectMessage> messages = notificationService.getDirectMessages(userId);
        return ApiResponse.success(messages);
    }

    /**
     * 用户发送私信
     * POST /notification/me/messages/direct
     */
    @PostMapping("/direct")
    public ApiResponse<DirectMessage> sendDirectMessage(@RequestBody Map<String, Object> body) {
        Long fromUserId = userContext.getCurrentUserId();
        if (fromUserId == null) {
            return ApiResponse.error(401, "用户未登录");
        }

        Object toUserIdObj = body.get("toUserId");
        Object contentObj = body.get("content");

        if (toUserIdObj == null || contentObj == null) {
            return ApiResponse.error(400, "参数不完整");
        }

        Long toUserId = Long.valueOf(toUserIdObj.toString());
        String content = contentObj.toString();

        if (toUserId.equals(fromUserId)) {
            return ApiResponse.error(400, "不能给自己发送私信");
        }

        DirectMessage message = notificationService.sendDirectMessage(fromUserId, toUserId, content);
        return ApiResponse.success(message);
    }

    /**
     * 用户获取点赞通知
     * GET /notification/me/messages/likes
     */
    @GetMapping("/likes")
    public ApiResponse<List<InboxMessage>> getLikeMessages() {
        Long userId = userContext.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        List<InboxMessage> messages = notificationService.getLikeMessages(userId);
        return ApiResponse.success(messages);
    }

    /**
     * 标记单条消息为已读
     * PUT /notification/me/messages/{id}/read
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
     * PUT /notification/me/messages/read-all
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

