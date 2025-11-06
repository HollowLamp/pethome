import React from "react";
import { Outlet } from "react-router";
import Searchbar from "./searchbar/searchbar";
import Me from "./me/me";
import styles from "./UserLayout.module.css"; // 导入 CSS Module

export default function UserLayout() {
  return (
    <div>
      <div className={styles.header}>
        {" "}
        <div className={styles.logo}>
          <img src="/logo.png" alt="logo" />
        </div>
        <div className={styles.left}>
          <div className={styles.menu}>宠物</div>
          <div className={styles.menu}>社区</div>
        </div>
        <Searchbar />
        <div className={styles.right}>
          <div className={styles.menu}>消息</div>
          <div className={styles.menu}>领养记录</div>
          <div className={styles.menu}>发帖记录</div>
        </div>
        <div className={styles.me}>
          <Me />
        </div>
      </div>
      <Outlet />
    </div>
  );
}
