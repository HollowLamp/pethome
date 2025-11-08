import React, { useState, useEffect } from "react";
import { Tabs, List, Badge, Empty, Spin, App as AntdApp, Button } from "antd";
import {
  BellOutlined,
  MessageOutlined,
  LikeOutlined,
  CheckOutlined,
} from "@ant-design/icons";
import api from "../../api";
import styles from "./Messages.module.css";

const { TabPane } = Tabs;

export default function UserMessages() {
  const { message } = AntdApp.useApp();
  const [activeTab, setActiveTab] = useState("system");
  const [loading, setLoading] = useState(false);
  const [systemMessages, setSystemMessages] = useState([]);
  const [directMessages, setDirectMessages] = useState([]);
  const [likeMessages, setLikeMessages] = useState([]);
  const [unreadCounts, setUnreadCounts] = useState({
    system: 0,
    direct: 0,
    likes: 0,
  });

  useEffect(() => {
    loadMessages();
  }, [activeTab]);

  const loadMessages = async () => {
    setLoading(true);
    try {
      if (activeTab === "system") {
        const res = await api.notification.getSystemMessages();
        setSystemMessages(res.data || []);
        // 统计未读数量（InboxMessage 有 isRead 字段）
        const unread = (res.data || []).filter((msg) => !msg.isRead).length;
        setUnreadCounts((prev) => ({ ...prev, system: unread }));
      } else if (activeTab === "direct") {
        const res = await api.notification.getDirectMessages();
        setDirectMessages(res.data || []);
        // 私信暂时不支持已读功能（DirectMessage 没有 isRead 字段）
        // 统计接收到的私信数量
        const currentUserId = getCurrentUserId();
        const unread = (res.data || []).filter(
          (msg) => msg.toUserId === currentUserId
        ).length;
        setUnreadCounts((prev) => ({ ...prev, direct: unread }));
      } else if (activeTab === "likes") {
        const res = await api.notification.getLikeMessages();
        setLikeMessages(res.data || []);
        // 统计未读数量（InboxMessage 有 isRead 字段）
        const unread = (res.data || []).filter((msg) => !msg.isRead).length;
        setUnreadCounts((prev) => ({ ...prev, likes: unread }));
      }
    } catch (error) {
      message.error("加载消息失败：" + (error.message || "未知错误"));
    } finally {
      setLoading(false);
    }
  };

  const getCurrentUserId = () => {
    // 从localStorage或store中获取当前用户ID
    const userStr = localStorage.getItem("user");
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        return user.id;
      } catch (e) {
        return null;
      }
    }
    return null;
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
  const handleMarkAsRead = async (messageId, messageType) => {
    try {
      if (messageType === "system" || messageType === "likes") {
        await api.notification.markMessageAsRead(messageId);

        // 立即更新本地状态（乐观更新），避免等待重新加载
        if (messageType === "system") {
          setSystemMessages((prev) =>
            prev.map((msg) =>
              msg.id === messageId ? { ...msg, isRead: true } : msg
            )
          );
          setUnreadCounts((prev) => ({
            ...prev,
            system: Math.max(0, prev.system - 1),
          }));
        } else if (messageType === "likes") {
          setLikeMessages((prev) =>
            prev.map((msg) =>
              msg.id === messageId ? { ...msg, isRead: true } : msg
            )
          );
          setUnreadCounts((prev) => ({
            ...prev,
            likes: Math.max(0, prev.likes - 1),
          }));
        }
      }
      // 私信暂时不支持标记已读（DirectMessage 没有 isRead 字段）
    } catch (error) {
      message.error("标记已读失败：" + (error.message || "未知错误"));
      // 如果失败，重新加载以恢复正确状态
      loadMessages();
    }
  };

  // 标记所有消息为已读
  const handleMarkAllAsRead = async () => {
    try {
      if (activeTab === "system" || activeTab === "likes") {
        await api.notification.markAllMessagesAsRead();

        // 立即更新本地状态（乐观更新）
        if (activeTab === "system") {
          setSystemMessages((prev) =>
            prev.map((msg) => ({ ...msg, isRead: true }))
          );
          setUnreadCounts((prev) => ({ ...prev, system: 0 }));
        } else if (activeTab === "likes") {
          setLikeMessages((prev) =>
            prev.map((msg) => ({ ...msg, isRead: true }))
          );
          setUnreadCounts((prev) => ({ ...prev, likes: 0 }));
        }

        message.success("所有消息已标记为已读");
      } else {
        message.info("私信暂不支持一键已读功能");
      }
    } catch (error) {
      message.error("标记已读失败：" + (error.message || "未知错误"));
      // 如果失败，重新加载以恢复正确状态
      loadMessages();
    }
  };

  const renderSystemMessage = (msg) => (
    <List.Item
      className={!msg.isRead ? styles.unreadItem : styles.readItem}
      onClick={() => {
        if (!msg.isRead) {
          handleMarkAsRead(msg.id, "system");
        }
      }}
      style={{ cursor: !msg.isRead ? "pointer" : "default" }}
    >
      <List.Item.Meta
        title={
          <div className={styles.messageTitle}>
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

  const renderDirectMessage = (msg) => {
    const isReceived = msg.toUserId === getCurrentUserId();
    return (
      <List.Item
        className={isReceived && !msg.isRead ? styles.unreadItem : styles.readItem}
      >
        <List.Item.Meta
          title={
            <div className={styles.messageTitle}>
              <span>{isReceived ? "收到私信" : "发送的私信"}</span>
              {isReceived && !msg.isRead && <Badge status="error" />}
            </div>
          }
          description={
            <div>
              <div className={styles.messageContent}>{msg.content}</div>
              <div className={styles.messageTime}>
                {formatTime(msg.createdAt)}
              </div>
            </div>
          }
        />
      </List.Item>
    );
  };

  const renderLikeMessage = (msg) => (
    <List.Item
      className={!msg.isRead ? styles.unreadItem : styles.readItem}
      onClick={() => {
        if (!msg.isRead) {
          handleMarkAsRead(msg.id, "likes");
        }
      }}
      style={{ cursor: !msg.isRead ? "pointer" : "default" }}
    >
      <List.Item.Meta
        title={
          <div className={styles.messageTitle}>
            <span>{msg.title || "点赞通知"}</span>
            {!msg.isRead && <Badge status="error" />}
          </div>
        }
        description={
          <div>
            <div className={styles.messageContent}>{msg.body || ""}</div>
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
        <h2>我的消息</h2>
      </div>
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        className={styles.messageTabs}
      >
        <TabPane
          tab={
            <span>
              <BellOutlined />
              系统通知
              {unreadCounts.system > 0 && (
                <Badge count={unreadCounts.system} offset={[8, 0]} />
              )}
            </span>
          }
          key="system"
        >
          <div style={{ marginBottom: 16, textAlign: "right" }}>
            <Button
              type="link"
              icon={<CheckOutlined />}
              onClick={handleMarkAllAsRead}
              disabled={unreadCounts.system === 0}
            >
              全部标记为已读
            </Button>
          </div>
          <Spin spinning={loading}>
            <List
              dataSource={systemMessages}
              renderItem={renderSystemMessage}
              locale={{ emptyText: <Empty description="暂无系统通知" /> }}
            />
          </Spin>
        </TabPane>
        <TabPane
          tab={
            <span>
              <MessageOutlined />
              私信
              {unreadCounts.direct > 0 && (
                <Badge count={unreadCounts.direct} offset={[8, 0]} />
              )}
            </span>
          }
          key="direct"
        >
          <Spin spinning={loading}>
            <List
              dataSource={directMessages}
              renderItem={renderDirectMessage}
              locale={{ emptyText: <Empty description="暂无私信" /> }}
            />
          </Spin>
        </TabPane>
        <TabPane
          tab={
            <span>
              <LikeOutlined />
              点赞通知
              {unreadCounts.likes > 0 && (
                <Badge count={unreadCounts.likes} offset={[8, 0]} />
              )}
            </span>
          }
          key="likes"
        >
          <div style={{ marginBottom: 16, textAlign: "right" }}>
            <Button
              type="link"
              icon={<CheckOutlined />}
              onClick={handleMarkAllAsRead}
              disabled={unreadCounts.likes === 0}
            >
              全部标记为已读
            </Button>
          </div>
          <Spin spinning={loading}>
            <List
              dataSource={likeMessages}
              renderItem={renderLikeMessage}
              locale={{ emptyText: <Empty description="暂无点赞通知" /> }}
            />
          </Spin>
        </TabPane>
      </Tabs>
    </div>
  );
}

