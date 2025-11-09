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
  Tooltip,
  Input,
} from "antd";
import {
  StarOutlined,
  StarFilled,
  EyeOutlined,
  LikeOutlined,
  MessageOutlined,
  SearchOutlined,
} from "@ant-design/icons";
import api from "../../../api";
import { processMediaUrls } from "../../../utils/imageUtils";

const { Title, Text, Paragraph } = Typography;

export default function FeaturedContent() {
  const { message } = AntdApp.useApp();
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [selectedPost, setSelectedPost] = useState(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [searchText, setSearchText] = useState("");

  const fetchPosts = async () => {
    setLoading(true);
    try {
      const res = await api.community.getPostList({
        page,
        pageSize,
        sort: "popular", // 按热度排序
      });
      if (res?.code === 200) {
        setPosts(res.data?.list || []);
        setTotal(res.data?.total || 0);
      }
    } catch (error) {
      message.error(error?.message || "获取帖子列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPosts();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, pageSize]);

  const handleToggleRecommend = async (postId, currentRecommend) => {
    try {
      const res = await api.community.toggleRecommend(
        postId,
        !currentRecommend
      );
      if (res?.code === 200) {
        message.success(currentRecommend ? "已取消推荐" : "已推荐到首页");
        fetchPosts();
      }
    } catch (error) {
      message.error(error?.message || "操作失败");
    }
  };

  const handleViewDetail = async (post) => {
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
      title: "互动数据",
      key: "stats",
      width: 200,
      render: (_, record) => (
        <Space direction="vertical" size={4}>
          <Space size={16}>
            <Tooltip title="点赞数">
              <Space size={4}>
                <LikeOutlined />
                <Text>{record.likeCount || 0}</Text>
              </Space>
            </Tooltip>
            <Tooltip title="评论数">
              <Space size={4}>
                <MessageOutlined />
                <Text>{record.commentCount || 0}</Text>
              </Space>
            </Tooltip>
          </Space>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {new Date(record.createdAt).toLocaleString()}
          </Text>
        </Space>
      ),
    },
    {
      title: "状态",
      dataIndex: "recommend",
      key: "recommend",
      width: 100,
      render: (recommend) =>
        recommend ? (
          <Tag icon={<StarFilled />} color="gold">
            已推荐
          </Tag>
        ) : (
          <Tag>未推荐</Tag>
        ),
      filters: [
        { text: "已推荐", value: true },
        { text: "未推荐", value: false },
      ],
      onFilter: (value, record) => record.recommend === value,
    },
    {
      title: "操作",
      key: "action",
      width: 180,
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
          {record.recommend ? (
            <Button
              size="small"
              danger
              icon={<StarOutlined />}
              onClick={() => handleToggleRecommend(record.id, record.recommend)}
            >
              取消推荐
            </Button>
          ) : (
            <Button
              size="small"
              type="primary"
              icon={<StarFilled />}
              onClick={() => handleToggleRecommend(record.id, record.recommend)}
            >
              推荐
            </Button>
          )}
        </Space>
      ),
    },
  ];

  const filteredPosts = searchText
    ? posts.filter(
        (post) =>
          post.title?.toLowerCase().includes(searchText.toLowerCase()) ||
          post.authorName?.toLowerCase().includes(searchText.toLowerCase())
      )
    : posts;

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
            推荐优秀案例
          </Title>
          <Space>
            <Input
              placeholder="搜索标题或作者"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              style={{ width: 250 }}
              allowClear
            />
            <Button onClick={fetchPosts}>刷新</Button>
          </Space>
        </div>

        <Table
          columns={columns}
          dataSource={filteredPosts}
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
        title="帖子详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailVisible(false)}>
            关闭
          </Button>,
          selectedPost?.recommend ? (
            <Button
              key="unrecommend"
              danger
              onClick={() => {
                handleToggleRecommend(selectedPost.id, selectedPost.recommend);
                setDetailVisible(false);
              }}
            >
              取消推荐
            </Button>
          ) : (
            <Button
              key="recommend"
              type="primary"
              onClick={() => {
                handleToggleRecommend(selectedPost.id, selectedPost.recommend);
                setDetailVisible(false);
              }}
            >
              推荐到首页
            </Button>
          ),
        ]}
        width={800}
      >
        {selectedPost && (
          <div>
            <Space direction="vertical" size={16} style={{ width: "100%" }}>
              <div>
                <Text type="secondary">标题：</Text>
                <Title level={5} style={{ margin: "4px 0" }}>
                  {selectedPost.title}
                </Title>
              </div>

              <div>
                <Text type="secondary">类型：</Text>
                <Tag
                  color={getTypeColor(selectedPost.type)}
                  style={{ marginLeft: 8 }}
                >
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
                      style={{
                        marginTop: 8,
                        display: "flex",
                        flexWrap: "wrap",
                        gap: 8,
                      }}
                    >
                      {processMediaUrls(selectedPost.mediaUrls).map(
                        (url, index) => (
                          <Image
                            key={index}
                            src={url}
                            alt={`媒体 ${index + 1}`}
                            width={150}
                            height={150}
                            style={{ objectFit: "cover", borderRadius: 8 }}
                            fallback="/images/post-placeholder.png"
                          />
                        )
                      )}
                    </div>
                  </div>
                )}

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

              <div>
                <Text type="secondary">推荐状态：</Text>
                {selectedPost.recommend ? (
                  <Tag
                    icon={<StarFilled />}
                    color="gold"
                    style={{ marginLeft: 8 }}
                  >
                    已推荐
                  </Tag>
                ) : (
                  <Tag style={{ marginLeft: 8 }}>未推荐</Tag>
                )}
              </div>
            </Space>
          </div>
        )}
      </Modal>
    </div>
  );
}
