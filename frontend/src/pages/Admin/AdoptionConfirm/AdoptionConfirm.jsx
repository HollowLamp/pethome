import React, { useEffect, useMemo, useState } from "react";
import {
  Card,
  Button,
  App as AntdApp,
  Typography,
  Space,
  Table,
  Tag,
  Modal,
  Empty,
  Descriptions,
} from "antd";
import { CheckOutlined, ReloadOutlined, EyeOutlined } from "@ant-design/icons";
import api from "../../../api";
import useAuthStore from "../../../store/authStore";

const { Title, Paragraph, Text } = Typography;

// 申请状态映射
const STATUS_MAP = {
  PENDING: { label: "待审核", color: "orange" },
  ORG_APPROVED: { label: "机构已通过", color: "green" },
  ORG_REJECTED: { label: "机构已拒绝", color: "red" },
  PLATFORM_APPROVED: { label: "平台已通过", color: "blue" },
  PLATFORM_REJECTED: { label: "平台已拒绝", color: "red" },
  COMPLETED: { label: "已完成", color: "default" },
};

export default function AdoptionConfirm() {
  const { message } = AntdApp.useApp();
  const { user } = useAuthStore();
  const userId = user?.id || user?.userId;
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [confirmVisible, setConfirmVisible] = useState(false);
  const [currentApp, setCurrentApp] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailData, setDetailData] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [interviewData, setInterviewData] = useState(null);
  const [interviewLoading, setInterviewLoading] = useState(false);
  const [currentOrg, setCurrentOrg] = useState(null);

  const fetchApplications = async () => {
    setLoading(true);
    try {
      // 查看平台已通过的申请（状态为 PLATFORM_APPROVED），等待确认交接
      const res = await api.adoption.getPendingApplications(
        "PLATFORM_APPROVED"
      );
      if (res?.code === 200) {
        const list = Array.isArray(res.data) ? res.data : res.data?.list || [];
        // 为每个申请获取面谈信息，只显示已完成面谈的申请
        const appsWithInterview = await Promise.all(
          list.map(async (app) => {
            try {
              const interviewRes = await api.interview.getInterviewRequest(
                app.id
              );
              if (
                interviewRes?.code === 200 &&
                interviewRes.data?.status === "DONE"
              ) {
                return { ...app, interview: interviewRes.data };
              }
            } catch {
              // 如果没有面谈预约或面谈未完成，不显示
            }
            return null;
          })
        );
        setApplications(appsWithInterview.filter(Boolean));
      }
    } catch (e) {
      message.error(e?.message || "获取申请列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApplications();
    if (userId) {
      fetchCurrentOrg();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fetchCurrentOrg = async () => {
    if (!userId) {
      setCurrentOrg(null);
      return;
    }
    try {
      const res = await api.org.getUserMemberships(userId);
      if (res?.code === 200) {
        const list = Array.isArray(res.data)
          ? res.data
          : res.data?.list || res.data?.memberships || [];
        if (list.length > 0) {
          const firstOrg = list[0].organizationId
            ? list[0]
            : list[0].org || list[0];
          setCurrentOrg({
            id: firstOrg.orgId || firstOrg.organizationId || firstOrg.id,
            name:
              firstOrg.name ||
              `机构 ${
                firstOrg.orgId || firstOrg.organizationId || firstOrg.id
              }`,
          });
        } else {
          setCurrentOrg(null);
        }
      }
    } catch (e) {
      console.warn("获取机构信息失败", e);
      setCurrentOrg(null);
    }
  };

  const openConfirmModal = (record) => {
    setCurrentApp(record);
    setConfirmVisible(true);
  };

  const handleConfirm = async () => {
    if (!currentApp) return;
    const appId = currentApp.id;
    if (!appId) {
      message.error("缺少申请ID");
      return;
    }
    setSubmitting(true);
    try {
      // 使用 interview API 完成交接
      const res = await api.interview.completeHandover(appId);
      if (res?.code === 200) {
        message.success("交接已完成");
        setConfirmVisible(false);
        fetchApplications();
      } else {
        message.error(res?.message || "操作失败");
      }
    } catch (e) {
      message.error(e?.message || "操作失败");
    } finally {
      setSubmitting(false);
    }
  };

  const handleViewDetail = async (record) => {
    setDetailVisible(true);
    setDetailLoading(true);
    setInterviewLoading(true);
    try {
      const [detailRes, interviewRes] = await Promise.all([
        api.adoption.getApplicationDetail(record.id),
        api.interview.getInterviewRequest(record.id).catch(() => null),
      ]);
      if (detailRes?.code === 200) {
        setDetailData(detailRes.data);
      } else {
        message.error(detailRes?.message || "获取详情失败");
      }
      if (interviewRes?.code === 200) {
        setInterviewData(interviewRes.data);
      } else {
        setInterviewData(null);
      }
    } catch (e) {
      message.error(e?.message || "获取详情失败");
    } finally {
      setDetailLoading(false);
      setInterviewLoading(false);
    }
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return "-";
    return new Date(dateTime).toLocaleString("zh-CN");
  };

  const columns = useMemo(
    () => [
      {
        title: "申请ID",
        dataIndex: "id",
        key: "id",
        width: 100,
      },
      {
        title: "宠物ID",
        dataIndex: "petId",
        key: "petId",
        width: 100,
      },
      {
        title: "申请人ID",
        dataIndex: "applicantId",
        key: "applicantId",
        width: 120,
      },
      {
        title: "状态",
        dataIndex: "status",
        key: "status",
        width: 140,
        render: (value) => {
          const status = STATUS_MAP[value] || {
            label: value,
            color: "default",
          };
          return <Tag color={status.color}>{status.label}</Tag>;
        },
      },
      {
        title: "提交时间",
        dataIndex: "createdAt",
        key: "createdAt",
        width: 180,
      },
      {
        title: "更新时间",
        dataIndex: "updatedAt",
        key: "updatedAt",
        width: 180,
      },
      {
        title: "操作",
        key: "action",
        width: 220,
        fixed: "right",
        render: (_, record) => (
          <Space>
            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleViewDetail(record)}
            >
              查看详情
            </Button>
            {record.status === "PLATFORM_APPROVED" &&
              record.interview?.status === "DONE" && (
                <Button
                  type="primary"
                  size="small"
                  icon={<CheckOutlined />}
                  onClick={() => openConfirmModal(record)}
                >
                  确认交接
                </Button>
              )}
          </Space>
        ),
      },
    ],
    []
  );

  return (
    <div>
      <Card style={{ marginBottom: 24 }}>
        <Space style={{ width: "100%", justifyContent: "space-between" }}>
          <div>
            <Title level={3} style={{ margin: 0 }}>
              确认领养交接
            </Title>
            <Paragraph
              type="secondary"
              style={{ marginTop: 8, marginBottom: 0 }}
            >
              机构管理员在线下完成宠物交接后，可在此页面确认交接完成。确认后申请状态将更新为"已完成"。
            </Paragraph>
          </div>
          {currentOrg && (
            <div style={{ textAlign: "right" }}>
              <Text type="secondary">当前机构：</Text>
              <Text strong style={{ marginLeft: 8 }}>
                {currentOrg.name} (ID: {currentOrg.id})
              </Text>
            </div>
          )}
        </Space>
      </Card>

      <Card
        title="待确认交接申请"
        extra={
          <Button
            icon={<ReloadOutlined />}
            onClick={fetchApplications}
            loading={loading}
          >
            刷新
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={applications}
          loading={loading}
          locale={{ emptyText: <Empty description="暂无待确认交接的申请" /> }}
          scroll={{ x: 1000 }}
        />
      </Card>

      <Modal
        title="确认交接完成"
        open={confirmVisible}
        onCancel={() => setConfirmVisible(false)}
        onOk={handleConfirm}
        okText="确认完成"
        okButtonProps={{ loading: submitting, type: "primary" }}
      >
        <Paragraph>
          确认线下交接已完成？确认后申请状态将更新为"已完成"，此操作不可撤销。
        </Paragraph>
        {currentApp && (
          <Descriptions
            column={1}
            bordered
            size="small"
            style={{ marginTop: 16 }}
          >
            <Descriptions.Item label="申请ID">
              {currentApp.id}
            </Descriptions.Item>
            <Descriptions.Item label="宠物ID">
              {currentApp.petId}
            </Descriptions.Item>
            <Descriptions.Item label="申请人ID">
              {currentApp.applicantId}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      <Modal
        title="申请详情"
        open={detailVisible}
        onCancel={() => {
          setDetailVisible(false);
          setDetailData(null);
          setInterviewData(null);
        }}
        footer={[
          <Button key="close" onClick={() => setDetailVisible(false)}>
            关闭
          </Button>,
        ]}
        width={900}
      >
        {detailLoading ? (
          <div style={{ textAlign: "center", padding: 40 }}>
            <Empty description="加载中..." />
          </div>
        ) : detailData ? (
          <>
            <Descriptions column={1} bordered>
              <Descriptions.Item label="申请ID">
                {detailData.id}
              </Descriptions.Item>
              <Descriptions.Item label="宠物ID">
                {detailData.petId}
              </Descriptions.Item>
              <Descriptions.Item label="申请人ID">
                {detailData.applicantId}
              </Descriptions.Item>
              <Descriptions.Item label="机构ID">
                {detailData.orgId}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                {STATUS_MAP[detailData.status]?.label || detailData.status}
              </Descriptions.Item>
              {detailData.rejectReason && (
                <Descriptions.Item label="拒绝原因">
                  {detailData.rejectReason}
                </Descriptions.Item>
              )}
              <Descriptions.Item label="创建时间">
                {formatDateTime(detailData.createdAt)}
              </Descriptions.Item>
              <Descriptions.Item label="更新时间">
                {formatDateTime(detailData.updatedAt)}
              </Descriptions.Item>
            </Descriptions>
            {interviewLoading ? (
              <div style={{ textAlign: "center", padding: 20, marginTop: 16 }}>
                <Empty description="加载面谈信息中..." />
              </div>
            ) : interviewData ? (
              <>
                <Paragraph style={{ marginTop: 24, marginBottom: 16 }}>
                  <strong>面谈信息</strong>
                </Paragraph>
                <Descriptions column={1} bordered>
                  <Descriptions.Item label="面谈状态">
                    {interviewData.status === "DONE"
                      ? "已完成"
                      : interviewData.status}
                  </Descriptions.Item>
                  {interviewData.slot && (
                    <>
                      <Descriptions.Item label="开始时间">
                        {formatDateTime(interviewData.slot.startAt)}
                      </Descriptions.Item>
                      <Descriptions.Item label="结束时间">
                        {formatDateTime(interviewData.slot.endAt)}
                      </Descriptions.Item>
                    </>
                  )}
                  <Descriptions.Item label="预约时间">
                    {formatDateTime(interviewData.createdAt)}
                  </Descriptions.Item>
                </Descriptions>
              </>
            ) : (
              <Paragraph style={{ marginTop: 24, textAlign: "center" }}>
                暂无面谈信息
              </Paragraph>
            )}
          </>
        ) : (
          <Empty description="暂无数据" />
        )}
      </Modal>
    </div>
  );
}
