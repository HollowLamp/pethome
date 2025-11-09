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
  Badge,
} from "antd";
import {
  EyeOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  WarningOutlined,
  MessageOutlined,
} from "@ant-design/icons";
import api from "../../../api";

const { Title, Text, Paragraph } = Typography;

/**
 * 裁定违规申诉
 * 超级管理员处理用户对删帖、状态处理的最终申诉
 * 这个页面汇总了所有待审核的举报，供管理员最终裁决
 */
export default function Appeals() {
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
      message.error(error?.message || "获取申诉列表失败");
    } finally {
      setLoading(false);
    }
  };

  // 最终裁决：批准举报，删除内容
  const handleApproveReport = async (report) => {
    modal.confirm({
      title: "确认裁决",
      content: (
        <div>
          <Paragraph>
            <WarningOutlined style={{ color: "#ff4d4f", marginRight: 8 }} />
            确认批准此举报并删除相关内容？此操作不可撤销。
          </Paragraph>
          <Paragraph>
            <Text strong>举报理由：</Text>
            <br />
            {report.reason}
          </Paragraph>
        </div>
      ),
      okText: "确认删除",
      okType: "danger",
      cancelText: "取消",
      onOk: async () => {
        setActionLoading(true);
        try {
          // 首先标记举报为已审核
          await api.community.handleReport(report.id, "REVIEWED");

          // 如果是帖子举报，删除帖子
          if (report.postId) {
            await api.community.updatePostStatus(report.postId, "REMOVED");
          }

          message.success("已裁决：内容已删除");
          fetchReports();
          setDetailVisible(false);
        } catch (error) {
          message.error(error?.message || "操作失败");
        } finally {
          setActionLoading(false);
        }
      },
    });
  };

  // 最终裁决：驳回举报，恢复内容
  const handleRejectReport = async (report) => {
    modal.confirm({
      title: "确认裁决",
      content: (
        <div>
          <Paragraph>
            <CheckCircleOutlined style={{ color: "#52c41a", marginRight: 8 }} />
            确认驳回此举报并恢复内容发布？
          </Paragraph>
          <Paragraph>
            <Text strong>举报理由：</Text>
            <br />
            {report.reason}
          </Paragraph>
        </div>
      ),
      okText: "确认驳回",
      cancelText: "取消",
      onOk: async () => {
        setActionLoading(true);
        try {
          // 首先标记举报为已审核
          await api.community.handleReport(report.id, "REVIEWED");

          // 如果是帖子举报，恢复帖子发布
          if (report.postId) {
            await api.community.updatePostStatus(report.postId, "PUBLISHED");
          }

          message.success("已裁决：举报已驳回，内容已恢复");
          fetchReports();
          setDetailVisible(false);
        } catch (error) {
          message.error(error?.message || "操作失败");
        } finally {
          setActionLoading(false);
        }
      },
    });
  };

  const handleViewDetail = async (report) => {
    setSelectedReport(report);
    setDetailVisible(true);
  };

  const columns = [
    {
      title: "举报ID",
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
            {record.postId && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                ID: {record.postId}
              </Text>
            )}
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
      render: (reason) => (
        <Text ellipsis style={{ maxWidth: 200 }}>
          {reason}
        </Text>
      ),
    },
    {
      title: "举报人",
      dataIndex: "reporterName",
      key: "reporterName",
      width: 120,
      render: (name) => name || "匿名",
    },
    {
      title: "内容作者",
      dataIndex: "targetAuthorName",
      key: "targetAuthorName",
      width: 120,
      render: (name) => name || "未知",
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (status) => {
        const statusMap = {
          PENDING: { text: "待裁决", color: "warning", icon: <ClockCircleOutlined /> },
          REVIEWED: { text: "已裁决", color: "success", icon: <CheckCircleOutlined /> },
        };
        const config = statusMap[status] || { text: status, color: "default" };
        return (
          <Tag color={config.color} icon={config.icon}>
            {config.text}
          </Tag>
        );
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
      width: 220,
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
              <Button
                type="primary"
                size="small"
                danger
                icon={<CheckCircleOutlined />}
                onClick={() => handleApproveReport(record)}
              >
                批准
              </Button>
              <Button
                size="small"
                icon={<CloseCircleOutlined />}
                onClick={() => handleRejectReport(record)}
              >
                驳回
              </Button>
            </>
          )}
        </Space>
      ),
    },
  ];

  const pendingCount = statusFilter === "" ? reports.filter(r => r.status === "PENDING").length :
                       statusFilter === "PENDING" ? total : 0;

  const tabItems = [
    {
      key: "PENDING",
      label: (
        <Badge count={pendingCount} offset={[10, 0]}>
          <span style={{ paddingRight: pendingCount > 0 ? 20 : 0 }}>待裁决</span>
        </Badge>
      ),
      icon: <ClockCircleOutlined />,
    },
    {
      key: "REVIEWED",
      label: "已裁决",
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
          <div>
            <Title level={4} style={{ margin: 0 }}>
              裁定违规申诉
            </Title>
            <Text type="secondary" style={{ fontSize: 13 }}>
              处理用户对删帖、状态处理的最终申诉
            </Text>
          </div>
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
            showTotal: (total) => `共 ${total} 条申诉`,
            onChange: (page, pageSize) => {
              setPage(page);
              setPageSize(pageSize);
            },
          }}
          scroll={{ x: 1300 }}
        />
      </Card>

      {/* 详情弹窗 */}
      <Modal
        title="申诉详情 - 最终裁决"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailVisible(false)}>
            关闭
          </Button>,
          selectedReport?.status === "PENDING" && (
            <>
              <Button
                key="reject"
                onClick={() => handleRejectReport(selectedReport)}
                loading={actionLoading}
              >
                驳回举报
              </Button>
              <Button
                key="approve"
                type="primary"
                danger
                onClick={() => handleApproveReport(selectedReport)}
                loading={actionLoading}
              >
                批准举报
              </Button>
            </>
          ),
        ]}
        width={800}
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
                <Tag
                  color={selectedReport.postId ? "blue" : "green"}
                  style={{ marginLeft: 8 }}
                >
                  {selectedReport.postId ? "帖子举报" : "评论举报"}
                </Tag>
              </div>

              {selectedReport.postId && (
                <div>
                  <Text type="secondary">帖子ID：</Text>
                  <Text style={{ marginLeft: 8 }}>{selectedReport.postId}</Text>
                </div>
              )}

              <div>
                <Text type="secondary">被举报内容：</Text>
                <Paragraph
                  style={{
                    marginTop: 8,
                    padding: 16,
                    background: "#f5f5f5",
                    borderRadius: 8,
                    border: "1px solid #d9d9d9",
                  }}
                >
                  {selectedReport.postTitle ||
                    selectedReport.commentContent ||
                    "未知内容"}
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
                <Paragraph
                  style={{
                    marginTop: 8,
                    padding: 16,
                    background: "#fff7e6",
                    borderRadius: 8,
                    border: "1px solid #ffd591",
                  }}
                >
                  <WarningOutlined
                    style={{ color: "#fa8c16", marginRight: 8 }}
                  />
                  {selectedReport.reason}
                </Paragraph>
              </div>

              <div>
                <Text type="secondary">举报人：</Text>
                <Text style={{ marginLeft: 8 }}>
                  {selectedReport.reporterName || "匿名"}
                </Text>
              </div>

              <div>
                <Text type="secondary">举报时间：</Text>
                <Text style={{ marginLeft: 8 }}>
                  {new Date(selectedReport.createdAt).toLocaleString()}
                </Text>
              </div>

              <div>
                <Text type="secondary">裁决状态：</Text>
                <Tag
                  color={
                    selectedReport.status === "PENDING" ? "warning" : "success"
                  }
                  style={{ marginLeft: 8 }}
                  icon={
                    selectedReport.status === "PENDING" ? (
                      <ClockCircleOutlined />
                    ) : (
                      <CheckCircleOutlined />
                    )
                  }
                >
                  {selectedReport.status === "PENDING" ? "待裁决" : "已裁决"}
                </Tag>
              </div>

              {selectedReport.status === "REVIEWED" &&
                selectedReport.reviewedAt && (
                  <div>
                    <Text type="secondary">裁决时间：</Text>
                    <Text style={{ marginLeft: 8 }}>
                      {new Date(selectedReport.reviewedAt).toLocaleString()}
                    </Text>
                  </div>
                )}

              {selectedReport.status === "PENDING" && (
                <div
                  style={{
                    marginTop: 16,
                    padding: 16,
                    background: "#e6f7ff",
                    borderRadius: 8,
                    border: "1px solid #91d5ff",
                  }}
                >
                  <Text strong style={{ color: "#0958d9" }}>
                    <MessageOutlined style={{ marginRight: 8 }} />
                    裁决说明：
                  </Text>
                  <ul style={{ marginTop: 8, marginBottom: 0 }}>
                    <li>
                      <Text>批准举报：将删除被举报的内容</Text>
                    </li>
                    <li>
                      <Text>驳回举报：将恢复内容的正常发布状态</Text>
                    </li>
                    <li>
                      <Text type="secondary">
                        此裁决为最终裁决，请谨慎操作
                      </Text>
                    </li>
                  </ul>
                </div>
              )}
            </Space>
          </div>
        )}
      </Modal>
    </div>
  );
}

