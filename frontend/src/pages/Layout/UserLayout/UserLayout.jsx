import React, { useState, useEffect } from "react";
import { Outlet, useLocation, useNavigate } from "react-router";
import { Badge, App as AntdApp } from "antd";
import { MessageOutlined } from "@ant-design/icons";
import Searchbar from "./searchbar/searchbar";
import Me from "./me/me";
import api from "../../../api";
import styles from "./UserLayout.module.css"; // 导入 CSS Module

export default function UserLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { message } = AntdApp.useApp();
  const [unreadCount, setUnreadCount] = useState(0);

  const activeKey = location.pathname.startsWith("/community") ? "community" : "pets";
  const isMessagesPage = location.pathname === "/messages";

  useEffect(() => {
    // 加载未读消息数量
    loadUnreadCount();
    // 设置定时刷新
    const timer = setInterval(loadUnreadCount, 30000); // 每30秒刷新一次
    return () => clearInterval(timer);
  }, []);

  const loadUnreadCount = async () => {
    try {
      // 获取所有类型的消息并统计未读数
      const [systemRes, directRes, likesRes] = await Promise.all([
        api.notification.getSystemMessages().catch(() => ({ data: [] })),
        api.notification.getDirectMessages().catch(() => ({ data: [] })),
        api.notification.getLikeMessages().catch(() => ({ data: [] })),
      ]);

      const systemUnread = (systemRes.data || []).filter((msg) => !msg.isRead).length;
      const directUnread = (directRes.data || []).filter((msg) => {
        const userStr = localStorage.getItem("user");
        if (userStr) {
          try {
            const user = JSON.parse(userStr);
            return msg.toUserId === user.id && !msg.isRead;
          } catch (e) {
            return false;
          }
        }
        return false;
      }).length;
      const likesUnread = (likesRes.data || []).filter((msg) => !msg.isRead).length;

      setUnreadCount(systemUnread + directUnread + likesUnread);
    } catch (error) {
      // 静默失败，不影响页面显示
      console.error("加载未读消息数量失败:", error);
    }
  };

  const handleMenuClick = (key) => {
    if (key === "pets") {
      navigate("/");
      return;
    }
    if (key === "community") {
      message.info("社区功能即将上线，敬请期待！");
      return;
    }
    if (key === "messages") {
      navigate("/messages");
      return;
    }
    message.info("功能建设中");
  };

  return (
    <div>
      <div className={styles.header}>
        <div className={styles.logo}>
          <img src="/logo.png" alt="logo" />
        </div>
        <div className={styles.left}>
          <div
            className={`${styles.menu} ${activeKey === "pets" ? styles.menuActive : ""}`}
            onClick={() => handleMenuClick("pets")}
            role="presentation"
          >
            宠物
          </div>
          <div
            className={`${styles.menu} ${activeKey === "community" ? styles.menuActive : ""}`}
            onClick={() => handleMenuClick("community")}
            role="presentation"
          >
            社区
          </div>
        </div>
        <Searchbar />
        <div className={styles.right}>
          <div
            className={`${styles.menu} ${isMessagesPage ? styles.menuActive : ""} ${unreadCount > 0 ? styles.hasUnread : ""}`}
            onClick={() => handleMenuClick("messages")}
            role="presentation"
            style={{ display: "flex", alignItems: "center", gap: 4 }}
          >
            {unreadCount > 0 ? (
              <Badge dot offset={[-2, 2]}>
                <MessageOutlined style={{ fontSize: 18 }} />
              </Badge>
            ) : (
              <MessageOutlined style={{ fontSize: 18 }} />
            )}
            <span>消息</span>
          </div>
          <div
            className={styles.menu}
            onClick={() => navigate("/adoption-record")}
            role="presentation"
          >
            领养记录
          </div>
          <div
            className={styles.menu}
            onClick={() => handleMenuClick("posts")}
            role="presentation"
          >
            发帖记录
          </div>
        </div>
        <div className={styles.me}>
          <Me />
        </div>
      </div>
      <Outlet />
    </div>
  );
}
