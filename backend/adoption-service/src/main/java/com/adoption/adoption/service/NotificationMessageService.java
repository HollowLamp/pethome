package com.adoption.adoption.service;

import com.adoption.adoption.config.RabbitMQConfig;
import com.adoption.adoption.feign.AuthServiceClient;
import com.adoption.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息通知服务
 *
 * 作用：封装 RabbitMQ 消息发送逻辑，提供简单易用的接口
 *
 * 使用场景：
 * - 当领养申请状态变更时，发送通知给相关用户
 * - 当申请被审核通过/拒绝时，通知申请人
 * - 当交接完成时，通知相关人员
 *
 * 消息格式：
 * {
 *   "eventType": "notify.system",  // 事件类型
 *   "payload": {                    // 消息内容
 *     "userId": 123,                // 接收者用户ID
 *     "title": "申请已通过",         // 通知标题
 *     "body": "您的领养申请已通过审核", // 通知内容
 *     "templateCode": "ADOPTION_APPROVED" // 模板代码
 *   }
 * }
 */
@Service
public class NotificationMessageService {

    private static final Logger log = LoggerFactory.getLogger(NotificationMessageService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthServiceClient authServiceClient;

    /**
     * 发送系统通知消息
     *
     * @param userId 接收者用户ID
     * @param title 通知标题
     * @param body 通知内容
     * @param templateCode 模板代码（可选，用于消息模板匹配）
     */
    public void sendSystemNotification(Long userId, String title, String body, String templateCode) {
        try {
            // 构建消息负载（payload）
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("title", title);
            payload.put("body", body);
            if (templateCode != null) {
                payload.put("templateCode", templateCode);
            }

            // 构建完整消息
            Map<String, Object> message = new HashMap<>();
            message.put("eventType", "notify.system"); // 事件类型：系统通知
            message.put("payload", payload);

            // 将消息对象转换为 JSON 字符串
            String messageJson = objectMapper.writeValueAsString(message);

            // 发送消息到 RabbitMQ
            // 参数说明：
            // 1. exchange: 交换机名称（notify）
            // 2. routingKey: 路由键（notify.system），用于匹配队列
            // 3. message: 消息内容（JSON 字符串）
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFY_EXCHANGE,
                    "notify.system",
                    messageJson
            );

            log.info("系统通知已发送: userId={}, title={}", userId, title);

        } catch (Exception e) {
            // 消息发送失败不应该影响主业务流程，只记录日志
            log.error("发送系统通知失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 发送系统通知消息（简化版，只传必要参数）
     *
     * @param userId 接收者用户ID
     * @param title 通知标题
     * @param body 通知内容
     */
    public void sendSystemNotification(Long userId, String title, String body) {
        sendSystemNotification(userId, title, body, null);
    }

    /**
     * 发送私信通知消息
     *
     * @param fromUserId 发送者用户ID
     * @param toUserId 接收者用户ID
     * @param content 私信内容
     */
    public void sendDirectMessage(Long fromUserId, Long toUserId, String content) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("fromUserId", fromUserId);
            payload.put("toUserId", toUserId);
            payload.put("content", content);

            Map<String, Object> message = new HashMap<>();
            message.put("eventType", "notify.direct"); // 事件类型：私信
            message.put("payload", payload);

            String messageJson = objectMapper.writeValueAsString(message);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFY_EXCHANGE,
                    "notify.direct",
                    messageJson
            );

            log.info("私信通知已发送: fromUserId={}, toUserId={}", fromUserId, toUserId);

        } catch (Exception e) {
            log.error("发送私信通知失败: fromUserId={}, toUserId={}, error={}",
                    fromUserId, toUserId, e.getMessage(), e);
        }
    }

    /**
     * 发送点赞通知消息
     *
     * @param userId 接收者用户ID
     * @param title 通知标题
     * @param body 通知内容
     */
    public void sendLikeNotification(Long userId, String title, String body) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("title", title);
            payload.put("body", body);

            Map<String, Object> message = new HashMap<>();
            message.put("eventType", "notify.likes"); // 事件类型：点赞通知
            message.put("payload", payload);

            String messageJson = objectMapper.writeValueAsString(message);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFY_EXCHANGE,
                    "notify.likes",
                    messageJson
            );

            log.info("点赞通知已发送: userId={}", userId);

        } catch (Exception e) {
            log.error("发送点赞通知失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 根据角色获取用户ID列表
     *
     * 通过调用 auth-service 获取所有拥有指定角色的用户ID
     *
     * @param role 角色名称（如：ORG_ADMIN, AUDITOR, CS, ADMIN, ORG_STAFF）
     * @return 用户ID列表
     */
    private List<Long> getUserIdsByRole(String role) {
        List<Long> userIds = new ArrayList<>();
        try {
            // 分页获取用户列表，每页100条
            int page = 1;
            int pageSize = 100;
            boolean hasMore = true;

            while (hasMore) {
                ApiResponse<Map<String, Object>> response = authServiceClient.getUserList(page, pageSize);

                if (response != null && response.getCode() == 200 && response.getData() != null) {
                    Map<String, Object> data = response.getData();
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> userList = (List<Map<String, Object>>) data.get("list");

                    if (userList != null) {
                        // 遍历用户列表，筛选出拥有指定角色的用户
                        for (Map<String, Object> user : userList) {
                            @SuppressWarnings("unchecked")
                            List<String> roles = (List<String>) user.get("roles");

                            if (roles != null && roles.contains(role)) {
                                Object idObj = user.get("id");
                                if (idObj != null) {
                                    Long userId = idObj instanceof Long ? (Long) idObj : Long.valueOf(idObj.toString());
                                    userIds.add(userId);
                                }
                            }
                        }
                    }

                    // 检查是否还有更多数据
                    Integer total = (Integer) data.get("total");
                    int currentCount = page * pageSize;
                    hasMore = total != null && currentCount < total;
                    page++;
                } else {
                    hasMore = false;
                }
            }

            log.info("获取角色用户列表: role={}, count={}", role, userIds.size());
        } catch (Exception e) {
            log.error("获取角色用户列表失败: role={}, error={}", role, e.getMessage(), e);
        }
        return userIds;
    }

    /**
     * 批量发送系统通知给指定角色的所有用户
     *
     * 使用场景：
     * - 发送给所有机构管理员（ORG_ADMIN）
     * - 发送给所有审核员（AUDITOR）
     * - 发送给所有客服人员（CS）
     * - 发送给所有超级管理员（ADMIN）
     * - 发送给所有宠物信息维护员（ORG_STAFF）
     *
     * @param role 角色名称
     * @param title 通知标题
     * @param body 通知内容
     * @param templateCode 模板代码（可选）
     */
    public void sendSystemNotificationToRole(String role, String title, String body, String templateCode) {
        try {
            // 获取拥有该角色的所有用户ID
            List<Long> userIds = getUserIdsByRole(role);

            if (userIds.isEmpty()) {
                log.warn("角色 {} 没有用户，跳过发送通知", role);
                return;
            }

            // 批量发送通知给每个用户
            for (Long userId : userIds) {
                sendSystemNotification(userId, title, body, templateCode);
            }

            log.info("批量发送系统通知完成: role={}, count={}, title={}", role, userIds.size(), title);
        } catch (Exception e) {
            log.error("批量发送系统通知失败: role={}, error={}", role, e.getMessage(), e);
        }
    }

    /**
     * 批量发送系统通知给指定角色的所有用户（简化版）
     *
     * @param role 角色名称
     * @param title 通知标题
     * @param body 通知内容
     */
    public void sendSystemNotificationToRole(String role, String title, String body) {
        sendSystemNotificationToRole(role, title, body, null);
    }

    /**
     * 发送系统通知给指定机构的机构管理员
     *
     * 注意：这个方法需要先通过 org-service 获取机构的管理员列表
     * 为了简化，这里先通过角色筛选，然后可以根据 orgId 进一步过滤
     *
     * @param orgId 机构ID
     * @param title 通知标题
     * @param body 通知内容
     * @param templateCode 模板代码（可选）
     */
    public void sendSystemNotificationToOrgAdmin(Long orgId, String title, String body, String templateCode) {
        try {
            // 获取所有机构管理员
            List<Long> orgAdminIds = getUserIdsByRole("ORG_ADMIN");

            // TODO: 这里应该通过 org-service 获取该机构的管理员列表
            // 暂时先发送给所有机构管理员，后续可以优化为只发送给该机构的管理员
            // 可以通过 org-service 的接口获取机构成员列表，然后筛选出 ORG_ADMIN 角色

            for (Long userId : orgAdminIds) {
                // 这里可以添加 orgId 过滤逻辑
                // 暂时先发送给所有机构管理员
                sendSystemNotification(userId, title, body, templateCode);
            }

            log.info("发送系统通知给机构管理员: orgId={}, count={}", orgId, orgAdminIds.size());
        } catch (Exception e) {
            log.error("发送系统通知给机构管理员失败: orgId={}, error={}", orgId, e.getMessage(), e);
        }
    }

    /**
     * 发送系统通知给指定机构的机构管理员（简化版）
     *
     * @param orgId 机构ID
     * @param title 通知标题
     * @param body 通知内容
     */
    public void sendSystemNotificationToOrgAdmin(Long orgId, String title, String body) {
        sendSystemNotificationToOrgAdmin(orgId, title, body, null);
    }
}

