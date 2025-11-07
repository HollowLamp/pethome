import React from "react";
import { Outlet, useLocation, useNavigate } from "react-router";
import { message } from "antd";
import Searchbar from "./searchbar/searchbar";
import Me from "./me/me";
import styles from "./UserLayout.module.css"; // 导入 CSS Module

export default function UserLayout() {
  const navigate = useNavigate();
  const location = useLocation();

  const activeKey = location.pathname.startsWith("/community") ? "community" : "pets";

  const handleMenuClick = (key) => {
    if (key === "pets") {
      navigate("/");
      return;
    }
    if (key === "community") {
      message.info("社区功能即将上线，敬请期待！");
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
            className={styles.menu}
            onClick={() => handleMenuClick("messages")}
            role="presentation"
          >
            消息
          </div>
          <div
            className={styles.menu}
            onClick={() => handleMenuClick("adoption-record")}
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
