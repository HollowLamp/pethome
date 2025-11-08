import React, { useState, useEffect } from "react";
import { List, Badge, Empty, Spin, App as AntdApp, Button } from "antd";
import { BellOutlined, CheckOutlined } from "@ant-design/icons";
import api from "../../api";
import styles from "./Messages.module.css";

export default function OrgMessages() {
  const { message } = AntdApp.useApp();
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    loadMessages();
  }, []);

  const loadMessages = async () => {
    setLoading(true);
    try {
      const res = await api.notification.getOrgSystemMessages();
      setMessages(res.data || []);
      // 统计未读数量
      const unread = (res.data || []).filter((msg) => !msg.isRead).length;
      setUnreadCount(unread);
    } catch (error) {
      message.error("加载消息失败：" + (error.message || "未知错误"));
    } finally {
      setLoading(false);
    }
  };

  const formatTime = (timeStr) => {
    if (!timeStr) return "";
    const date = new Date(timeStr);
    const now = new Date();
    const diff = now - date;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return "刚刚";
    if (minutes < 60) return `${minutes}分钟前`;
    if (hours < 24) return `${hours}小时前`;
    if (days < 7) return `${days}天前`;
    return date.toLocaleDateString();
  };

  // 标记单条消息为已读
  const handleMarkAsRead = async (messageId) => {
    try {
      await api.notification.markOrgMessageAsRead(messageId);

      // 立即更新本地状态（乐观更新），避免等待重新加载
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === messageId ? { ...msg, isRead: true } : msg
        )
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (error) {
      message.error("标记已读失败：" + (error.message || "未知错误"));
      // 如果失败，重新加载以恢复正确状态
      loadMessages();
    }
  };

  // 标记所有消息为已读
  const handleMarkAllAsRead = async () => {
    try {
      await api.notification.markAllOrgMessagesAsRead();

      // 立即更新本地状态（乐观更新）
      setMessages((prev) =>
        prev.map((msg) => ({ ...msg, isRead: true }))
      );
      setUnreadCount(0);

      message.success("所有消息已标记为已读");
    } catch (error) {
      message.error("标记已读失败：" + (error.message || "未知错误"));
      // 如果失败，重新加载以恢复正确状态
      loadMessages();
    }
  };

  const renderMessage = (msg) => (
    <List.Item
      className={!msg.isRead ? styles.unreadItem : styles.readItem}
      onClick={() => {
        if (!msg.isRead) {
          handleMarkAsRead(msg.id);
        }
      }}
      style={{ cursor: !msg.isRead ? "pointer" : "default" }}
    >
      <List.Item.Meta
        title={
          <div className={styles.messageTitle}>
            <BellOutlined style={{ marginRight: 8 }} />
            <span>{msg.title || msg.templateCode || "系统通知"}</span>
            {!msg.isRead && <Badge status="error" />}
          </div>
        }
        description={
          <div>
            <div className={styles.messageContent}>
              {msg.body || (msg.payload ? JSON.parse(msg.payload).body || "" : "")}
            </div>
            <div className={styles.messageTime}>
              {formatTime(msg.createdAt)}
            </div>
          </div>
        }
      />
    </List.Item>
  );

  return (
    <div className={styles.messagesContainer}>
      <div className={styles.messagesHeader}>
        <h2>
          <BellOutlined style={{ marginRight: 8 }} />
          系统通知
          {unreadCount > 0 && (
            <Badge count={unreadCount} style={{ marginLeft: 8 }} />
          )}
        </h2>
      </div>
      <div style={{ marginBottom: 16, textAlign: "right" }}>
        <Button
          type="link"
          icon={<CheckOutlined />}
          onClick={handleMarkAllAsRead}
          disabled={unreadCount === 0}
        >
          全部标记为已读
        </Button>
      </div>
      <Spin spinning={loading}>
        <List
          dataSource={messages}
          renderItem={renderMessage}
          locale={{ emptyText: <Empty description="暂无系统通知" /> }}
        />
      </Spin>
    </div>
  );
}

