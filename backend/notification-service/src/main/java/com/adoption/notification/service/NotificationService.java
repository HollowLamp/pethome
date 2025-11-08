package com.adoption.notification.service;

import com.adoption.notification.model.DirectMessage;
import com.adoption.notification.model.InboxMessage;
import com.adoption.notification.model.NotifyTask;
import com.adoption.notification.repository.DirectMessageMapper;
import com.adoption.notification.repository.InboxMessageMapper;
import com.adoption.notification.repository.NotifyTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotifyTaskMapper notifyTaskMapper;

    @Autowired
    private InboxMessageMapper inboxMessageMapper;

    @Autowired
    private DirectMessageMapper directMessageMapper;

    /**
     * 获取系统通知（从收件箱消息中获取，因为收件箱消息有已读标记）
     * 系统通知会同时创建 NotifyTask 和 InboxMessage，这里返回 InboxMessage 以便支持已读功能
     */
    public List<InboxMessage> getSystemMessages(Long userId) {
        // 从收件箱消息中获取系统通知（排除点赞通知）
        List<InboxMessage> allMessages = inboxMessageMapper.selectByUserId(userId);
        return allMessages.stream()
                .filter(msg -> {
                    // 排除点赞通知（点赞通知的 title 包含"点赞"）
                    String title = msg.getTitle();
                    return title == null || !title.contains("点赞");
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取私信消息
     */
    public List<DirectMessage> getDirectMessages(Long userId) {
        return directMessageMapper.selectByUserId(userId);
    }

    /**
     * 发送私信
     */
    public DirectMessage sendDirectMessage(Long fromUserId, Long toUserId, String content) {
        DirectMessage message = new DirectMessage();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        directMessageMapper.insert(message);
        return message;
    }

    /**
     * 获取点赞通知（从收件箱中筛选）
     */
    public List<InboxMessage> getLikeMessages(Long userId) {
        // 这里可以根据 title 或 body 中包含"点赞"关键词来筛选
        // 或者使用单独的字段来标识消息类型
        List<InboxMessage> allMessages = inboxMessageMapper.selectByUserId(userId);
        return allMessages.stream()
                .filter(msg -> msg.getTitle() != null && msg.getTitle().contains("点赞"))
                .collect(Collectors.toList());
    }

    /**
     * 创建系统通知任务
     */
    public NotifyTask createNotifyTask(Long userId, String channel, String templateCode, String payload) {
        NotifyTask task = new NotifyTask();
        task.setUserId(userId);
        task.setChannel(channel != null ? channel : "SYSTEM");
        task.setTemplateCode(templateCode);
        task.setPayload(payload);
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setCreatedAt(LocalDateTime.now());
        notifyTaskMapper.insert(task);
        return task;
    }

    /**
     * 创建收件箱消息
     */
    public InboxMessage createInboxMessage(Long toUserId, String title, String body) {
        InboxMessage message = new InboxMessage();
        message.setToUserId(toUserId);
        message.setTitle(title);
        message.setBody(body);
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        inboxMessageMapper.insert(message);
        return message;
    }

    /**
     * 标记消息为已读
     */
    public void markMessageAsRead(Long messageId) {
        int updated = inboxMessageMapper.markAsRead(messageId);
        if (updated == 0) {
            throw new RuntimeException("消息不存在或已被标记为已读: " + messageId);
        }
    }

    /**
     * 标记所有消息为已读
     */
    public void markAllMessagesAsRead(Long userId) {
        inboxMessageMapper.markAllAsRead(userId);
    }

    /**
     * 更新通知任务状态
     */
    public void updateNotifyTaskStatus(NotifyTask task) {
        notifyTaskMapper.updateStatus(task);
    }
}

