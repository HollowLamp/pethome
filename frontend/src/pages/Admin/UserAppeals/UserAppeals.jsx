import React, { useState, useEffect } from "react";
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  App as AntdApp,
  Modal,
  Typography,
  Tabs,
  Input,
  Radio,
  Image,
} from "antd";
import {
  EyeOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
} from "@ant-design/icons";
import api from "../../../api";
import { processMediaUrls } from "../../../utils/imageUtils";

const { Title, Text, Paragraph, TextArea } = Typography;

export default function UserAppeals() {
  const { message, modal } = AntdApp.useApp();
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [selectedReport, setSelectedReport] = useState(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [statusFilter, setStatusFilter] = useState("PENDING");
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    fetchReports();
  }, [page, pageSize, statusFilter]);

  const fetchReports = async () => {
    setLoading(true);
    try {
      const res = await api.community.getReports({
        page,
        pageSize,
        status: statusFilter,
      });
      if (res?.code === 200) {
        setReports(res.data?.list || []);
        setTotal(res.data?.total || 0);
      }
    } catch (error) {
      message.error(error?.message || "获取举报列表失败");
    } finally {
      setLoading(false);
    }
  };

  const handleReview = async (reportId, status) => {
    setActionLoading(true);
    try {
      const res = await api.community.handleReport(reportId, status);
      if (res?.code === 200) {
        message.success("处理成功");
        fetchReports();
        setDetailVisible(false);
      }
    } catch (error) {
      message.error(error?.message || "处理失败");
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdatePostStatus = async (postId, status, statusLabel) => {
    setActionLoading(true);
    try {
      const res = await api.community.updatePostStatus(postId, status);
      if (res?.code === 200) {
        message.success(`已${statusLabel}`);
        fetchReports();
        setDetailVisible(false);
      }
    } catch (error) {
      message.error(error?.message || "操作失败");
    } finally {
      setActionLoading(false);
    }
  };

  const showPostStatusModal = (report) => {
    if (!report.postId) {
      message.warning("该申诉不是针对帖子的");
      return;
    }

    let selectedAction = "PUBLISHED";

    modal.confirm({
      title: "处理帖子状态",
      content: (
        <div>
          <Paragraph>
            <Text strong>帖子标题：</Text>
            {report.postTitle || "未知"}
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
        handleUpdatePostStatus(report.postId, selectedAction, statusMap[selectedAction]);
      },
    });
  };

  const showActionModal = (report, action) => {
    const actionText = action === "REVIEWED" ? "标记为已处理" : "驳回";

    modal.confirm({
      title: `确认${actionText}`,
      content: (
        <div>
          <Paragraph>
            <Text strong>举报理由：</Text>
            {report.reason}
          </Paragraph>
          <Paragraph>
            <Text strong>举报人：</Text>
            {report.reporterName || "匿名"}
          </Paragraph>
        </div>
      ),
      okText: "确定",
      cancelText: "取消",
      onOk: () => handleReview(report.id, action),
    });
  };

  const handleViewDetail = async (report) => {
    setSelectedReport(report);
    setDetailVisible(true);
  };

  const columns = [
    {
      title: "ID",
      dataIndex: "id",
      key: "id",
      width: 80,
    },
    {
      title: "举报内容",
      key: "target",
      width: 250,
      render: (_, record) => (
        <div>
          <div style={{ fontWeight: 600, marginBottom: 4 }}>
            {record.postTitle || record.commentContent || "未知内容"}
          </div>
          <Space size={4}>
            <Tag color={record.postId ? "blue" : "green"} style={{ fontSize: 11 }}>
              {record.postId ? "帖子" : "评论"}
            </Tag>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {record.targetAuthorName || "未知作者"}
            </Text>
          </Space>
        </div>
      ),
    },
    {
      title: "举报理由",
      dataIndex: "reason",
      key: "reason",
      width: 200,
      ellipsis: true,
    },
    {
      title: "举报人",
      dataIndex: "reporterName",
      key: "reporterName",
      width: 120,
      render: (name) => name || "匿名",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (status) => {
        const statusMap = {
          PENDING: { text: "待处理", color: "warning" },
          REVIEWED: { text: "已处理", color: "success" },
        };
        const config = statusMap[status] || { text: status, color: "default" };
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: "举报时间",
      dataIndex: "createdAt",
      key: "createdAt",
      width: 150,
      render: (createdAt) => new Date(createdAt).toLocaleString(),
    },
    {
      title: "操作",
      key: "action",
      width: 200,
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
          {record.status === "PENDING" && (
            <>
              {record.postId && (
                <Button
                  type="default"
                  size="small"
                  onClick={() => showPostStatusModal(record)}
                >
                  处理帖子
                </Button>
              )}
              <Button
                type="primary"
                size="small"
                icon={<CheckCircleOutlined />}
                onClick={() => showActionModal(record, "REVIEWED")}
              >
                已处理
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  const tabItems = [
    {
      key: "PENDING",
      label: "待处理",
      icon: <ClockCircleOutlined />,
    },
    {
      key: "REVIEWED",
      label: "已处理",
      icon: <CheckCircleOutlined />,
    },
    {
      key: "",
      label: "全部",
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
            处理用户申诉
          </Title>
          <Button onClick={fetchReports}>刷新</Button>
        </div>

        <Tabs
          activeKey={statusFilter}
          items={tabItems}
          onChange={(key) => {
            setStatusFilter(key);
            setPage(1);
          }}
        />

        <Table
          columns={columns}
          dataSource={reports}
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
        title="举报详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailVisible(false)}>
            关闭
          </Button>,
          selectedReport?.status === "PENDING" && selectedReport?.postId && (
            <Button
              key="handlePost"
              type="default"
              loading={actionLoading}
              onClick={() => {
                setDetailVisible(false);
                showPostStatusModal(selectedReport);
              }}
            >
              处理帖子状态
            </Button>
          ),
          selectedReport?.status === "PENDING" && (
            <Button
              key="handle"
              type="primary"
              loading={actionLoading}
              onClick={() => showActionModal(selectedReport, "REVIEWED")}
            >
              标记为已处理
            </Button>
          ),
        ]}
        width={700}
      >
        {selectedReport && (
          <div>
            <Space direction="vertical" size={16} style={{ width: "100%" }}>
              <div>
                <Text type="secondary">举报ID：</Text>
                <Text style={{ marginLeft: 8 }}>{selectedReport.id}</Text>
              </div>

              <div>
                <Text type="secondary">举报类型：</Text>
                <Tag color={selectedReport.postId ? "blue" : "green"} style={{ marginLeft: 8 }}>
                  {selectedReport.postId ? "帖子举报" : "评论举报"}
                </Tag>
              </div>

              <div>
                <Text type="secondary">举报内容：</Text>
                <Paragraph style={{ marginTop: 8, padding: 12, background: "#f5f5f5", borderRadius: 8 }}>
                  {selectedReport.postTitle || selectedReport.commentContent || "未知内容"}
                </Paragraph>
              </div>

              <div>
                <Text type="secondary">内容作者：</Text>
                <Text style={{ marginLeft: 8 }}>
                  {selectedReport.targetAuthorName || "未知"}
                </Text>
              </div>

              <div>
                <Text type="secondary">举报理由：</Text>
                <Paragraph style={{ marginTop: 8, padding: 12, background: "#fff7e6", borderRadius: 8 }}>
                  {selectedReport.reason}
                </Paragraph>
              </div>

              <div>
                <Text type="secondary">举报人：</Text>
                <Text style={{ marginLeft: 8 }}>{selectedReport.reporterName || "匿名"}</Text>
              </div>

              <div>
                <Text type="secondary">举报时间：</Text>
                <Text style={{ marginLeft: 8 }}>
                  {new Date(selectedReport.createdAt).toLocaleString()}
                </Text>
              </div>

              <div>
                <Text type="secondary">处理状态：</Text>
                <Tag
                  color={selectedReport.status === "PENDING" ? "warning" : "success"}
                  style={{ marginLeft: 8 }}
                >
                  {selectedReport.status === "PENDING" ? "待处理" : "已处理"}
                </Tag>
              </div>

              {selectedReport.status === "REVIEWED" && selectedReport.reviewedAt && (
                <div>
                  <Text type="secondary">处理时间：</Text>
                  <Text style={{ marginLeft: 8 }}>
                    {new Date(selectedReport.reviewedAt).toLocaleString()}
                  </Text>
                </div>
              )}
            </Space>
          </div>
        )}
      </Modal>
    </div>
  );
}

