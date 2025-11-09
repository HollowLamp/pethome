import React, { useEffect, useState } from "react";
import {
  Card,
  Typography,
  Space,
  Tag,
  Button,
  Divider,
  List,
  Skeleton,
  Empty,
  Modal,
  Input,
  App as AntdApp,
  Avatar,
  Popconfirm,
} from "antd";
import {
  LikeOutlined,
  LikeFilled,
  MessageOutlined,
  ClockCircleOutlined,
  WarningOutlined,
  DeleteOutlined,
  ArrowLeftOutlined,
} from "@ant-design/icons";
import { useParams, useNavigate } from "react-router";
import api from "../../../api";
import useAuthStore from "../../../store/authStore";
import PetCard from "../../Pets/components/PetCard";
import { processMediaUrls, processImageUrl } from "../../../utils/imageUtils";
import styles from "./PostDetail.module.css";

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

const TYPE_OPTIONS = {
  DAILY: { label: "养宠日常", color: "blue" },
  GUIDE: { label: "养宠攻略", color: "green" },
  PET_PUBLISH: { label: "宠物发布", color: "orange" },
};

export default function PostDetail() {
  const { message, modal } = AntdApp.useApp();
  const { postId } = useParams();
  const navigate = useNavigate();

  const { isLoggedIn, user } = useAuthStore();

  const [post, setPost] = useState(null);
  const [loading, setLoading] = useState(false);
  const [comments, setComments] = useState([]);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [commentText, setCommentText] = useState("");
  const [submittingComment, setSubmittingComment] = useState(false);
  const [likeLoading, setLikeLoading] = useState(false);
  const [bindPet, setBindPet] = useState(null);
  const [loadingPet, setLoadingPet] = useState(false);

  const fetchPostDetail = async () => {
    setLoading(true);
    try {
      const res = await api.community.getPostDetail(postId);
      if (res?.code === 200) {
        setPost(res.data);
        // 如果有绑定的宠物ID，获取宠物详情
        if (res.data.bindPetId) {
          fetchBindPet(res.data.bindPetId);
        }
      }
    } catch (error) {
      message.error(error?.message || "获取帖子详情失败");
    } finally {
      setLoading(false);
    }
  };

  const fetchBindPet = async (petId) => {
    setLoadingPet(true);
    try {
      const res = await api.pets.getPetDetail(petId);
      if (res?.code === 200) {
        setBindPet(res.data);
      }
    } catch (error) {
      console.error("获取关联宠物失败:", error);
    } finally {
      setLoadingPet(false);
    }
  };

  const fetchComments = async () => {
    setCommentsLoading(true);
    try {
      const res = await api.community.getComments(postId, {
        page: 1,
        pageSize: 100,
      });
      if (res?.code === 200) {
        setComments(res.data?.list || []);
      }
    } catch (error) {
      message.error(error?.message || "获取评论失败");
    } finally {
      setCommentsLoading(false);
    }
  };

  useEffect(() => {
    fetchPostDetail();
    fetchComments();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [postId]);

  const handleLike = async () => {
    if (!isLoggedIn) {
      message.warning("请先登录");
      return;
    }

    setLikeLoading(true);
    try {
      const res = await api.community.togglePostLike(postId);
      if (res?.code === 200) {
        setPost((prev) => ({
          ...prev,
          isLiked: res.data?.isLiked,
          likeCount: res.data?.likeCount || prev.likeCount,
        }));
        message.success(res.data?.isLiked ? "点赞成功" : "取消点赞");
      }
    } catch (error) {
      message.error(error?.message || "操作失败");
    } finally {
      setLikeLoading(false);
    }
  };

  const handleCommentLike = async (commentId) => {
    if (!isLoggedIn) {
      message.warning("请先登录");
      return;
    }

    try {
      const res = await api.community.toggleCommentLike(commentId);
      if (res?.code === 200) {
        setComments((prev) =>
          prev.map((comment) =>
            comment.id === commentId
              ? {
                  ...comment,
                  isLiked: res.data?.isLiked,
                  likeCount: res.data?.likeCount || comment.likeCount,
                }
              : comment
          )
        );
      }
    } catch (error) {
      message.error(error?.message || "操作失败");
    }
  };

  const handleSubmitComment = async () => {
    if (!isLoggedIn) {
      message.warning("请先登录");
      return;
    }

    if (!commentText.trim()) {
      message.warning("请输入评论内容");
      return;
    }

    setSubmittingComment(true);
    try {
      const res = await api.community.createComment(postId, {
        content: commentText,
      });
      if (res?.code === 200) {
        message.success("评论成功");
        setCommentText("");
        fetchComments();
      }
    } catch (error) {
      message.error(error?.message || "评论失败");
    } finally {
      setSubmittingComment(false);
    }
  };

  const handleDeleteComment = async (commentId) => {
    try {
      const res = await api.community.deleteComment(commentId);
      if (res?.code === 200) {
        message.success("删除成功");
        fetchComments();
      }
    } catch (error) {
      message.error(error?.message || "删除失败");
    }
  };

  const handleDeletePost = async () => {
    try {
      const res = await api.community.deletePost(postId);
      if (res?.code === 200) {
        message.success("删除成功");
        navigate("/community");
      }
    } catch (error) {
      message.error(error?.message || "删除失败");
    }
  };

  const handleReport = () => {
    if (!isLoggedIn) {
      message.warning("请先登录");
      return;
    }

    let reason = "";
    modal.confirm({
      title: "举报帖子",
      content: (
        <TextArea
          rows={4}
          placeholder="请输入举报理由"
          onChange={(e) => {
            reason = e.target.value;
          }}
        />
      ),
      onOk: async () => {
        if (!reason.trim()) {
          message.warning("请输入举报理由");
          return Promise.reject();
        }

        try {
          const res = await api.community.reportPost(postId, {
            reason: reason,
          });
          if (res?.code === 200) {
            message.success("举报成功，我们会尽快处理");
          }
        } catch (error) {
          message.error(error?.message || "举报失败");
          return Promise.reject();
        }
      },
    });
  };

  const formatNumber = (num) => {
    if (num >= 10000) return `${(num / 10000).toFixed(1)}万`;
    return num;
  };

  // 打字机效果组件
  const TypewriterText = ({ text, speed = 50 }) => {
    const [displayedText, setDisplayedText] = useState("");
    const [currentIndex, setCurrentIndex] = useState(0);

    useEffect(() => {
      if (!text) return;

      if (currentIndex < text.length) {
        const timer = setTimeout(() => {
          setDisplayedText(text.substring(0, currentIndex + 1));
          setCurrentIndex(currentIndex + 1);
        }, speed);
        return () => clearTimeout(timer);
      }
    }, [currentIndex, text, speed]);

    useEffect(() => {
      // 重置状态
      setDisplayedText("");
      setCurrentIndex(0);
    }, [text]);

    return <span>{displayedText}</span>;
  };

  if (loading) {
    return (
      <div className={styles.page}>
        <div className={styles.container}>
          <Card>
            <Skeleton active avatar paragraph={{ rows: 8 }} />
          </Card>
        </div>
      </div>
    );
  }

  if (!post) {
    return (
      <div className={styles.page}>
        <div className={styles.container}>
          <Empty description="帖子不存在" />
        </div>
      </div>
    );
  }

  const mediaUrls = processMediaUrls(post.mediaUrls);
  const typeInfo = TYPE_OPTIONS[post.type] || {
    label: post.type,
    color: "default",
  };
  const isAuthor = user?.id === post.authorId;

  // 渲染右侧边栏内容
  const renderSidebar = () => {
    // 养宠日常和宠物发布：显示宠物
    if ((post.type === "DAILY" || post.type === "PET_PUBLISH") && bindPet) {
      return (
        <Card
          className={styles.sidebarCard}
          title={post.type === "PET_PUBLISH" ? "📢 关联宠物" : "🐾 我的宠物"}
        >
          {loadingPet ? (
            <Skeleton active avatar paragraph={{ rows: 2 }} />
          ) : (
            <PetCard
              pet={bindPet}
              onClick={() => navigate(`/pets/${bindPet.id}`)}
            />
          )}
        </Card>
      );
    }

    // 养宠攻略：显示AI总结（如果有）
    if (post.type === "GUIDE" && post.aiSummary) {
      return (
        <Card className={styles.sidebarCard} title="🤖 AI 总结">
          <div className={styles.aiSummaryContent}>
            <TypewriterText text={post.aiSummary} speed={30} />
            <span className={styles.cursor}>|</span>
          </div>
        </Card>
      );
    }

    return null;
  };

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate("/community")}
          style={{ marginBottom: 16 }}
        >
          返回列表
        </Button>

        <div className={styles.contentLayout}>
          <div className={styles.mainContent}>
            <Card className={styles.postCard}>
              {/* 帖子头部 */}
              <div className={styles.postHeader}>
                <Space size={12}>
                  <Avatar
                    src={processImageUrl(post.authorAvatarUrl)}
                    size={48}
                    style={{ backgroundColor: "#ff7f5d" }}
                  >
                    {post.authorName?.[0] || "U"}
                  </Avatar>
                  <div>
                    <div className={styles.authorName}>
                      {post.authorName || "匿名用户"}
                    </div>
                    <Space size={12} className={styles.postMeta}>
                      <span>
                        <ClockCircleOutlined />
                        {new Date(post.createdAt).toLocaleString()}
                      </span>
                    </Space>
                  </div>
                </Space>
                <Space>
                  <Tag color={typeInfo.color}>{typeInfo.label}</Tag>
                  {isAuthor && (
                    <Popconfirm
                      title="确定删除这篇帖子吗？"
                      onConfirm={handleDeletePost}
                      okText="确定"
                      cancelText="取消"
                    >
                      <Button
                        type="text"
                        danger
                        icon={<DeleteOutlined />}
                        size="small"
                      >
                        删除
                      </Button>
                    </Popconfirm>
                  )}
                </Space>
              </div>

              <Divider />

              {/* 帖子内容 */}
              <div className={styles.postContent}>
                <Title level={2} className={styles.postTitle}>
                  {post.title}
                </Title>
                <Paragraph className={styles.postText}>
                  {post.content}
                </Paragraph>

                {/* 媒体展示 */}
                {mediaUrls.length > 0 && (
                  <div className={styles.mediaGrid}>
                    {mediaUrls.map((url, index) => {
                      const isVideo = url
                        .toLowerCase()
                        .match(/\.(mp4|avi|mov|wmv|flv|webm)$/);
                      return (
                        <div key={index} className={styles.mediaItem}>
                          {isVideo ? (
                            <video
                              src={url}
                              controls
                              style={{
                                width: "100%",
                                height: "100%",
                                objectFit: "cover",
                              }}
                            />
                          ) : (
                            <img
                              src={url}
                              alt={`媒体 ${index + 1}`}
                              onError={(e) => {
                                // 如果加载失败，尝试其他URL格式
                                const originalUrl = e.target.src;
                                if (!originalUrl.includes("/files/")) {
                                  e.target.src = `/files/${originalUrl.replace(
                                    /^.*\/files\//,
                                    ""
                                  )}`;
                                } else {
                                  e.target.onerror = null;
                                  e.target.style.display = "none";
                                }
                              }}
                            />
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              <Divider />

              {/* 互动按钮 */}
              <div className={styles.actions}>
                <Space size={24}>
                  <Button
                    type={post.isLiked ? "primary" : "default"}
                    icon={post.isLiked ? <LikeFilled /> : <LikeOutlined />}
                    loading={likeLoading}
                    onClick={handleLike}
                  >
                    {formatNumber(post.likeCount || 0)}
                  </Button>
                  <Button icon={<MessageOutlined />}>
                    {formatNumber(comments.length || 0)} 评论
                  </Button>
                </Space>
                <Button
                  type="text"
                  danger
                  icon={<WarningOutlined />}
                  onClick={handleReport}
                >
                  举报
                </Button>
              </div>
            </Card>

            {/* 评论区 */}
            <Card className={styles.commentsCard} title="评论">
              {isLoggedIn ? (
                <div className={styles.commentInput}>
                  <TextArea
                    rows={4}
                    placeholder="写下你的评论..."
                    value={commentText}
                    onChange={(e) => setCommentText(e.target.value)}
                    maxLength={500}
                    showCount
                  />
                  <Button
                    type="primary"
                    onClick={handleSubmitComment}
                    loading={submittingComment}
                    style={{ marginTop: 12 }}
                  >
                    发表评论
                  </Button>
                </div>
              ) : (
                <div className={styles.loginPrompt}>
                  <Text type="secondary">登录后才能评论哦</Text>
                  <Button
                    type="link"
                    onClick={() =>
                      window.dispatchEvent(new Event("OPEN_LOGIN_MODAL"))
                    }
                  >
                    立即登录
                  </Button>
                </div>
              )}

              <Divider />

              {commentsLoading ? (
                <Skeleton active avatar paragraph={{ rows: 2 }} />
              ) : comments.length === 0 ? (
                <Empty description="暂无评论，来抢沙发吧~" />
              ) : (
                <List
                  dataSource={comments}
                  renderItem={(comment) => (
                    <List.Item
                      key={comment.id}
                      actions={[
                        <Button
                          type="text"
                          icon={
                            comment.isLiked ? <LikeFilled /> : <LikeOutlined />
                          }
                          onClick={() => handleCommentLike(comment.id)}
                        >
                          {comment.likeCount || 0}
                        </Button>,
                        user?.id === comment.userId && (
                          <Popconfirm
                            title="确定删除这条评论吗？"
                            onConfirm={() => handleDeleteComment(comment.id)}
                            okText="确定"
                            cancelText="取消"
                          >
                            <Button
                              type="text"
                              danger
                              icon={<DeleteOutlined />}
                            >
                              删除
                            </Button>
                          </Popconfirm>
                        ),
                      ].filter(Boolean)}
                    >
                      <List.Item.Meta
                        avatar={
                          <Avatar
                            src={processImageUrl(comment.userAvatarUrl)}
                            style={{ backgroundColor: "#1677ff" }}
                          >
                            {comment.userName?.[0] || "U"}
                          </Avatar>
                        }
                        title={
                          <Space>
                            <Text strong>{comment.userName || "匿名用户"}</Text>
                            <Text type="secondary" style={{ fontSize: 12 }}>
                              {new Date(comment.createdAt).toLocaleString()}
                            </Text>
                          </Space>
                        }
                        description={comment.content}
                      />
                    </List.Item>
                  )}
                />
              )}
            </Card>
          </div>

          {/* 右侧边栏 */}
          <div className={styles.sidebar}>{renderSidebar()}</div>
        </div>
      </div>
    </div>
  );
}
