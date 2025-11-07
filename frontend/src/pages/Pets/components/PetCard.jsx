import React from "react";
import { Tag } from "antd";
import { HeartOutlined, EnvironmentOutlined } from "@ant-design/icons";
import styles from "./PetCard.module.css";

const TYPE_LABELS = {
  DOG: "狗狗",
  CAT: "猫咪",
  RABBIT: "兔子",
  BIRD: "小鸟",
  HAMSTER: "仓鼠",
  GUINEA_PIG: "豚鼠",
  TURTLE: "乌龟",
  FISH: "鱼",
  OTHER: "其他",
};

const STATUS_LABELS = {
  AVAILABLE: "可领养",
  RESERVED: "已预约",
  ADOPTED: "已领养",
  ARCHIVED: "已下架",
};

export default function PetCard({ pet, onClick }) {
  const cover = pet.coverUrl
    ? pet.coverUrl.startsWith("http")
      ? pet.coverUrl
      : `/files/${pet.coverUrl}`
    : "/images/pet-placeholder.png";

  return (
    <div className={styles.card} onClick={onClick} role="presentation">
      <div className={styles.coverWrapper}>
        <img src={cover} alt={pet.name} className={styles.cover} />
        <div className={styles.coverMask}>
          <span className={styles.coverAction}>查看详情</span>
        </div>
      </div>
      <div className={styles.body}>
        <div className={styles.titleRow}>
          <span className={styles.name}>{pet.name || "未命名宠物"}</span>
          <Tag color="gold" className={styles.typeTag}>
            {TYPE_LABELS[pet.type] || pet.type || "未知"}
          </Tag>
        </div>
        <div className={styles.metaRow}>
          <span>{pet.breed || "品种未填"}</span>
          {pet.age != null && <span>{pet.age} 岁</span>}
        </div>
        <div className={styles.metaRow}>
          <span className={styles.iconRow}>
            <EnvironmentOutlined />
            <span>{pet.orgName || "未知机构"}</span>
          </span>
          <Tag color="cyan" className={styles.statusTag}>
            {STATUS_LABELS[pet.status] || pet.status || "-"}
          </Tag>
        </div>
      </div>
      <div className={styles.footer}>
        <HeartOutlined className={styles.icon} />
        <span className={styles.footerText}>收藏人数：{pet.favCount ?? 0}</span>
      </div>
    </div>
  );
}
