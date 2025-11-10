import React, { useState, useEffect } from "react";
import {
  Card,
  Empty,
  Skeleton,
  App as AntdApp,
  Button,
  Space,
  Tag,
  Popconfirm,
} from "antd";
import {
  PlusOutlined,
  LikeOutlined,
  MessageOutlined,
  ClockCircleOutlined,
  DeleteOutlined,
  FireOutlined,
  WarningOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router";
import api from "../../../api";
import { processMediaUrls } from "../../../utils/imageUtils";
import styles from "./MyPosts.module.css";

const TYPE_OPTIONS = {
  DAILY: { label: "养宠日常", color: "blue" },
  GUIDE: { label: "养宠攻略", color: "green" },
  PET_PUBLISH: { label: "宠物发布", color: "orange" },
};

export default function MyPosts() {
  const { message } = AntdApp.useApp();
  const navigate = useNavigate();

  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);

  const fetchMyPosts = async () => {
    setLoading(true);
    try {
      const res = await api.community.getMyPosts({
        page,
        pageSize: 20,
      });
      if (res?.code === 200) {
        const list = res.data?.list || [];
        setPosts(list);
        const total = res.data?.total ?? 0;
        setHasMore(page * 20 < total);
      } else {
        // 如果响应码不是 200，停止加载更多
        setHasMore(false);
      }
    } catch (error) {
      message.error(error?.message || "获取发帖记录失败");
      // 请求失败时停止加载更多
      setHasMore(false);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMyPosts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const handleDelete = async (postId) => {
    try {
      const res = await api.community.deletePost(postId);
      if (res?.code === 200) {
        message.success("删除成功");
        fetchMyPosts();
      }
    } catch (error) {
      message.error(error?.message || "删除失败");
    }
  };


  const formatNumber = (num) => {
    if (num >= 10000) return `${(num / 10000).toFixed(1)}万`;
    return num;
  };

  const getStatusTag = (status) => {
    const statusMap = {
      PUBLISHED: { text: "已发布", color: "success" },
      FLAGGED: { text: "违规", color: "warning" },
      REMOVED: { text: "已删除", color: "error" },
    };
    const config = statusMap[status] || { text: status, color: "default" };
    return <Tag color={config.color}>{config.text}</Tag>;
  };

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        <div className={styles.header}>
          <h1>我的帖子</h1>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            size="large"
            onClick={() => navigate("/community/create")}
          >
            发布新帖
          </Button>
        </div>

        {loading && posts.length === 0 ? (
          <div className={styles.postsContainer}>
            {Array.from({ length: 6 }).map((_, index) => (
              <Card key={index} className={styles.postCard}>
                <Skeleton active avatar paragraph={{ rows: 3 }} />
              </Card>
            ))}
          </div>
        ) : posts.length === 0 ? (
          <Card className={styles.emptyCard}>
            <Empty
              description="还没有发布过帖子"
              image={Empty.PRESENTED_IMAGE_SIMPLE}
            >
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => navigate("/community/create")}
              >
                发布第一篇帖子
              </Button>
            </Empty>
          </Card>
        ) : (
          <div className={styles.postsContainer}>
            {posts.map((post) => {
              const mediaUrls = processMediaUrls(post.mediaUrls);
              const firstImage = mediaUrls[0];
              const typeInfo = TYPE_OPTIONS[post.type] || {
                label: post.type,
                color: "default",
              };

              return (
                <Card
                  key={post.id}
                  className={styles.postCard}
                  hoverable
                  onClick={() => navigate(`/community/${post.id}`)}
                >
                  <div className={styles.postHeader}>
                    <Space size={8}>
                      <Tag color={typeInfo.color}>{typeInfo.label}</Tag>
                      {getStatusTag(post.status)}
                      {post.aiFlagged && (
                        <Tag icon={<WarningOutlined />} color="warning">
                          AI标记
                        </Tag>
                      )}
                    </Space>
                    <Popconfirm
                      title="确定删除这篇帖子吗？"
                      onConfirm={(e) => {
                        e?.stopPropagation();
                        handleDelete(post.id);
                      }}
                      onCancel={(e) => e?.stopPropagation()}
                      okText="确定"
                      cancelText="取消"
                    >
                      <Button
                        type="text"
                        danger
                        icon={<DeleteOutlined />}
                        size="small"
                        onClick={(e) => e.stopPropagation()}
                      />
                    </Popconfirm>
                  </div>

                  <div className={styles.postContent}>
                    <h3 className={styles.postTitle}>{post.title}</h3>
                    <p className={styles.postExcerpt}>
                      {post.content?.substring(0, 120)}
                      {post.content?.length > 120 ? "..." : ""}
                    </p>

                    {firstImage && (
                      <div className={styles.postImage}>
                        <img
                          src={firstImage}
                          alt={post.title}
                          onError={(e) => {
                            // 如果加载失败，尝试其他URL格式
                            const originalUrl = e.target.src;
                            if (!originalUrl.includes("/files/")) {
                              e.target.src = `/files/${originalUrl.replace(/^.*\/files\//, "")}`;
                            } else {
                              e.target.onerror = null;
                              e.target.style.display = "none";
                            }
                          }}
                        />
                        {mediaUrls.length > 1 && (
                          <div className={styles.imageCount}>
                            {mediaUrls.length} 图
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  <div className={styles.postMeta}>
                    <Space size={4} className={styles.time}>
                      <ClockCircleOutlined />
                      <span>
                        {new Date(post.createdAt).toLocaleDateString()}
                      </span>
                    </Space>
                  </div>

                  <div className={styles.postFooter}>
                    <Space size={24}>
                      <span className={styles.stat}>
                        <LikeOutlined />
                        {formatNumber(post.likeCount || 0)}
                      </span>
                      <span className={styles.stat}>
                        <MessageOutlined />
                        {formatNumber(post.commentCount || 0)}
                      </span>
                    </Space>
                    {post.isRecommended && (
                      <Tag color="red" icon={<FireOutlined />}>
                        推荐
                      </Tag>
                    )}
                  </div>
                </Card>
              );
            })}
          </div>
        )}

        {hasMore && posts.length > 0 && (
          <div className={styles.loadMore}>
            <Button
              onClick={() => setPage((p) => p + 1)}
              loading={loading}
              size="large"
            >
              加载更多
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}

