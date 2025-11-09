import React, { useState, useEffect } from "react";
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  Image,
  App as AntdApp,
  Modal,
  Typography,
  Radio,
} from "antd";
import {
  EyeOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  WarningOutlined,
  LikeOutlined,
  MessageOutlined,
} from "@ant-design/icons";
import api from "../../../api";
import { processMediaUrls } from "../../../utils/imageUtils";

const { Title, Text, Paragraph } = Typography;

export default function ViolationReview() {
  const { message, modal } = AntdApp.useApp();
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [selectedPost, setSelectedPost] = useState(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    fetchFlaggedPosts();
  }, [page, pageSize]);

  const fetchFlaggedPosts = async () => {
    setLoading(true);
    try {
      const res = await api.community.getFlaggedPosts({
        page,
        pageSize,
      });
      if (res?.code === 200) {
        setPosts(res.data?.list || []);
        setTotal(res.data?.total || 0);
      }
    } catch (error) {
      message.error(error?.message || "获取违规帖子列表失败");
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (postId, status, statusLabel) => {
    setActionLoading(true);
    try {
      const res = await api.community.updatePostStatus(postId, status);
      if (res?.code === 200) {
        message.success(`已${statusLabel}`);
        fetchFlaggedPosts();
        setDetailVisible(false);
      }
    } catch (error) {
      message.error(error?.message || "操作失败");
    } finally {
      setActionLoading(false);
    }
  };

  const showActionModal = (post) => {
    let selectedAction = "PUBLISHED";

    modal.confirm({
      title: "处理违规内容",
      content: (
        <div>
          <Paragraph>
            <Text strong>帖子标题：</Text>
            {post.title}
          </Paragraph>
          <Paragraph>
            <Text strong>处理方式：</Text>
          </Paragraph>
          <Radio.Group
            defaultValue="PUBLISHED"
            onChange={(e) => {
              selectedAction = e.target.value;
            }}
          >
            <Space direction="vertical">
              <Radio value="PUBLISHED">恢复发布（内容正常）</Radio>
              <Radio value="FLAGGED">标记违规（保持违规状态）</Radio>
              <Radio value="REMOVED">删除（永久移除）</Radio>
            </Space>
          </Radio.Group>
        </div>
      ),
      width: 500,
      okText: "确定",
      cancelText: "取消",
      onOk: () => {
        const statusMap = {
          PUBLISHED: "恢复发布",
          FLAGGED: "标记违规",
          REMOVED: "删除",
        };
        handleUpdateStatus(post.id, selectedAction, statusMap[selectedAction]);
      },
    });
  };

  const handleViewDetail = (post) => {
    setSelectedPost(post);
    setDetailVisible(true);
  };

  const getTypeLabel = (type) => {
    const typeMap = {
      DAILY: "养宠日常",
      GUIDE: "养宠攻略",
      PET_PUBLISH: "宠物发布",
    };
    return typeMap[type] || type;
  };

  const getTypeColor = (type) => {
    const colorMap = {
      DAILY: "blue",
      GUIDE: "green",
      PET_PUBLISH: "orange",
    };
    return colorMap[type] || "default";
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

  const columns = [
    {
      title: "ID",
      dataIndex: "id",
      key: "id",
      width: 80,
    },
    {
      title: "标题",
      dataIndex: "title",
      key: "title",
      width: 300,
      render: (text, record) => (
        <div>
          <div style={{ fontWeight: 600, marginBottom: 4 }}>{text}</div>
          <Space size={4}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {record.authorName || "匿名用户"}
            </Text>
            <Tag color={getTypeColor(record.type)} style={{ fontSize: 11 }}>
              {getTypeLabel(record.type)}
            </Tag>
          </Space>
        </div>
      ),
    },
    {
      title: "封面",
      dataIndex: "mediaUrls",
      key: "mediaUrls",
      width: 100,
      render: (mediaUrls) => {
        const urls = processMediaUrls(mediaUrls);
        if (urls.length > 0) {
          return (
            <Image
              src={urls[0]}
              alt="封面"
              width={60}
              height={60}
              style={{ objectFit: "cover", borderRadius: 8 }}
              fallback="/images/post-placeholder.png"
            />
          );
        }
        return <Text type="secondary">无</Text>;
      },
    },
    {
      title: "AI标记",
      dataIndex: "aiFlagged",
      key: "aiFlagged",
      width: 100,
      render: (aiFlagged) =>
        aiFlagged ? (
          <Tag icon={<WarningOutlined />} color="warning">
            AI标记
          </Tag>
        ) : (
          <Tag>正常</Tag>
        ),
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (status) => getStatusTag(status),
    },
    {
      title: "互动数据",
      key: "stats",
      width: 150,
      render: (_, record) => (
        <Space size={12}>
          <Space size={4}>
            <LikeOutlined />
            <Text>{record.likeCount || 0}</Text>
          </Space>
          <Space size={4}>
            <MessageOutlined />
            <Text>{record.commentCount || 0}</Text>
          </Space>
        </Space>
      ),
    },
    {
      title: "发布时间",
      dataIndex: "createdAt",
      key: "createdAt",
      width: 150,
      render: (createdAt) => new Date(createdAt).toLocaleString(),
    },
    {
      title: "操作",
      key: "action",
      width: 150,
      fixed: "right",
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            查看
          </Button>
          <Button
            type="primary"
            size="small"
            onClick={() => showActionModal(record)}
          >
            处理
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card>
        <div
          style={{
            marginBottom: 16,
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <Title level={4} style={{ margin: 0 }}>
            复核违规内容
          </Title>
          <Button onClick={fetchFlaggedPosts}>刷新</Button>
        </div>

        <Table
          columns={columns}
          dataSource={posts}
          rowKey="id"
          loading={loading}
          pagination={{
            current: page,
            pageSize: pageSize,
            total: total,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => {
              setPage(page);
              setPageSize(pageSize);
            },
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* 详情弹窗 */}
      <Modal
        title="违规内容详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailVisible(false)}>
            关闭
          </Button>,
          <Button
            key="handle"
            type="primary"
            loading={actionLoading}
            onClick={() => {
              setDetailVisible(false);
              showActionModal(selectedPost);
            }}
          >
            处理
          </Button>,
        ]}
        width={800}
      >
        {selectedPost && (
          <div>
            <Space direction="vertical" size={16} style={{ width: "100%" }}>
              <div>
                <Text type="secondary">帖子ID：</Text>
                <Text style={{ marginLeft: 8 }}>{selectedPost.id}</Text>
              </div>

              <div>
                <Text type="secondary">标题：</Text>
                <Title level={5} style={{ margin: "4px 0" }}>
                  {selectedPost.title}
                </Title>
              </div>

              <div>
                <Text type="secondary">类型：</Text>
                <Tag color={getTypeColor(selectedPost.type)} style={{ marginLeft: 8 }}>
                  {getTypeLabel(selectedPost.type)}
                </Tag>
              </div>

              <div>
                <Text type="secondary">作者：</Text>
                <Text style={{ marginLeft: 8 }}>
                  {selectedPost.authorName || "匿名用户"}
                </Text>
              </div>

              <div>
                <Text type="secondary">内容：</Text>
                <Paragraph style={{ marginTop: 8, whiteSpace: "pre-wrap" }}>
                  {selectedPost.content}
                </Paragraph>
              </div>

              {selectedPost.mediaUrls &&
                processMediaUrls(selectedPost.mediaUrls).length > 0 && (
                  <div>
                    <Text type="secondary">媒体文件：</Text>
                    <div
                      style={{ marginTop: 8, display: "flex", flexWrap: "wrap", gap: 8 }}
                    >
                      {processMediaUrls(selectedPost.mediaUrls).map((url, index) => (
                        <Image
                          key={index}
                          src={url}
                          alt={`媒体 ${index + 1}`}
                          width={150}
                          height={150}
                          style={{ objectFit: "cover", borderRadius: 8 }}
                          fallback="/images/post-placeholder.png"
                        />
                      ))}
                    </div>
                  </div>
                )}

              <div>
                <Text type="secondary">AI标记：</Text>
                {selectedPost.aiFlagged ? (
                  <Tag icon={<WarningOutlined />} color="warning" style={{ marginLeft: 8 }}>
                    AI已标记为违规
                  </Tag>
                ) : (
                  <Tag style={{ marginLeft: 8 }}>AI未标记</Tag>
                )}
              </div>

              <div>
                <Text type="secondary">当前状态：</Text>
                <span style={{ marginLeft: 8 }}>{getStatusTag(selectedPost.status)}</span>
              </div>

              <div>
                <Text type="secondary">互动数据：</Text>
                <Space size={16} style={{ marginLeft: 8 }}>
                  <Space size={4}>
                    <LikeOutlined />
                    <Text>{selectedPost.likeCount || 0} 点赞</Text>
                  </Space>
                  <Space size={4}>
                    <MessageOutlined />
                    <Text>{selectedPost.commentCount || 0} 评论</Text>
                  </Space>
                </Space>
              </div>

              <div>
                <Text type="secondary">发布时间：</Text>
                <Text style={{ marginLeft: 8 }}>
                  {new Date(selectedPost.createdAt).toLocaleString()}
                </Text>
              </div>
            </Space>
          </div>
        )}
      </Modal>
    </div>
  );
}

