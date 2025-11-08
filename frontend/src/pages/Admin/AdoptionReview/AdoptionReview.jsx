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
  Input,
  Empty,
  Descriptions,
  List,
  Divider,
} from "antd";
import {
  CheckOutlined,
  CloseOutlined,
  ReloadOutlined,
  EyeOutlined,
  FileTextOutlined,
} from "@ant-design/icons";
import api from "../../../api";

const { Title, Paragraph } = Typography;

// 申请状态映射
const STATUS_MAP = {
  PENDING: { label: "待审核", color: "orange" },
  ORG_APPROVED: { label: "机构已通过", color: "green" },
  ORG_REJECTED: { label: "机构已拒绝", color: "red" },
  PLATFORM_APPROVED: { label: "平台已通过", color: "blue" },
  PLATFORM_REJECTED: { label: "平台已拒绝", color: "red" },
  COMPLETED: { label: "已完成", color: "default" },
};

// 材料类型映射
const DOC_TYPE_MAP = {
  ID_CARD: "身份证",
  INCOME_PROOF: "收入证明",
  PET_HISTORY: "养宠历史",
  HOUSING_PROOF: "住房证明",
  OTHER: "其他材料",
};

export default function AdoptionReview() {
  const { message } = AntdApp.useApp();
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [reasonVisible, setReasonVisible] = useState(false);
  const [reason, setReason] = useState("");
  const [currentApp, setCurrentApp] = useState(null);
  const [actionType, setActionType] = useState("approve");
  const [submitting, setSubmitting] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailData, setDetailData] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [applicationDocs, setApplicationDocs] = useState([]);
  const [docsLoading, setDocsLoading] = useState(false);
  const [previewVisible, setPreviewVisible] = useState(false);
  const [previewUrl, setPreviewUrl] = useState("");

  const fetchApplications = async () => {
    setLoading(true);
    try {
      // 审核员复审：查看机构已通过的申请（状态为 ORG_APPROVED）
      const res = await api.adoption.getPendingApplications("ORG_APPROVED");
      if (res?.code === 200) {
        const list = Array.isArray(res.data) ? res.data : res.data?.list || [];
        setApplications(list);
      }
    } catch (e) {
      message.error(e?.message || "获取申请列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApplications();
  }, []);

  const openReasonModal = (record, type) => {
    setCurrentApp(record);
    setActionType(type);
    setReason("");
    setReasonVisible(true);
  };

  const handleSubmit = async () => {
    if (!currentApp) return;
    const appId = currentApp.id;
    if (!appId) {
      message.error("缺少申请ID");
      return;
    }
    setSubmitting(true);
    try {
      if (actionType === "approve") {
        const res = await api.adoption.platformApproveApplication(appId);
        if (res?.code === 200) {
          message.success("申请平台审核已批准");
          setReasonVisible(false);
          fetchApplications();
        } else {
          message.error(res?.message || "操作失败");
        }
      } else {
        if (!reason.trim()) {
          message.warning("拒绝申请必须填写原因");
          setSubmitting(false);
          return;
        }
        const res = await api.adoption.platformRejectApplication(
          appId,
          reason.trim()
        );
        if (res?.code === 200) {
          message.success("申请平台审核已拒绝");
          setReasonVisible(false);
          fetchApplications();
        } else {
          message.error(res?.message || "操作失败");
        }
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
    setDocsLoading(true);
    try {
      const [detailRes, docsRes] = await Promise.all([
        api.adoption.getApplicationDetail(record.id),
        api.adoption.getApplicationDocs(record.id),
      ]);
      if (detailRes?.code === 200) {
        setDetailData(detailRes.data);
      } else {
        message.error(detailRes?.message || "获取详情失败");
      }
      if (docsRes?.code === 200) {
        setApplicationDocs(Array.isArray(docsRes.data) ? docsRes.data : []);
      }
    } catch (e) {
      message.error(e?.message || "获取详情失败");
    } finally {
      setDetailLoading(false);
      setDocsLoading(false);
    }
  };

  const handlePreview = (url) => {
    const fullUrl = url.startsWith("http") ? url : `/files/${url}`;
    setPreviewUrl(fullUrl);
    setPreviewVisible(true);
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
        title: "机构ID",
        dataIndex: "orgId",
        key: "orgId",
        width: 100,
      },
      {
        title: "状态",
        dataIndex: "status",
        key: "status",
        width: 140,
        render: (value) => {
          const status = STATUS_MAP[value] || { label: value, color: "default" };
          return <Tag color={status.color}>{status.label}</Tag>;
        },
      },
      {
        title: "拒绝原因",
        dataIndex: "rejectReason",
        key: "rejectReason",
        ellipsis: true,
        render: (value) => value || "-",
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
        width: 280,
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
            {record.status === "ORG_APPROVED" && (
              <>
                <Button
                  type="primary"
                  size="small"
                  icon={<CheckOutlined />}
                  onClick={() => openReasonModal(record, "approve")}
                >
                  通过
                </Button>
                <Button
                  danger
                  size="small"
                  icon={<CloseOutlined />}
                  onClick={() => openReasonModal(record, "reject")}
                >
                  拒绝
                </Button>
              </>
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
        <Title level={3}>审核领养申请（复审）</Title>
        <Paragraph type="secondary">
          审核员可对机构管理员已通过的领养申请进行平台复审，确保申请材料真实有效，避免违规或虚假材料。通过后申请将进入交接阶段；若拒绝，需填写拒绝原因。
        </Paragraph>
      </Card>

      <Card
        title="待复审申请"
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
          locale={{ emptyText: <Empty description="暂无待复审申请" /> }}
          scroll={{ x: 1200 }}
        />
      </Card>

      <Modal
        title={actionType === "approve" ? "复审通过" : "复审拒绝"}
        open={reasonVisible}
        onCancel={() => setReasonVisible(false)}
        onOk={handleSubmit}
        okText={actionType === "approve" ? "确认通过" : "确认拒绝"}
        okButtonProps={{ loading: submitting, danger: actionType === "reject" }}
      >
        {actionType === "reject" && (
          <Input.TextArea
            rows={4}
            maxLength={500}
            placeholder="请填写拒绝原因（必填）"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            showCount
          />
        )}
        {actionType === "approve" && (
          <Paragraph>
            确认通过此申请？通过后申请将进入交接阶段，机构管理员可安排面谈并确认交接。
          </Paragraph>
        )}
      </Modal>

      <Modal
        title="申请详情"
        open={detailVisible}
        onCancel={() => {
          setDetailVisible(false);
          setDetailData(null);
          setApplicationDocs([]);
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
              <Descriptions.Item label="申请ID">{detailData.id}</Descriptions.Item>
              <Descriptions.Item label="宠物ID">{detailData.petId}</Descriptions.Item>
              <Descriptions.Item label="申请人ID">
                {detailData.applicantId}
              </Descriptions.Item>
              <Descriptions.Item label="机构ID">{detailData.orgId}</Descriptions.Item>
              <Descriptions.Item label="状态">
                {STATUS_MAP[detailData.status]?.label || detailData.status}
              </Descriptions.Item>
              {detailData.rejectReason && (
                <Descriptions.Item label="拒绝原因">
                  {detailData.rejectReason}
                </Descriptions.Item>
              )}
              <Descriptions.Item label="创建时间">
                {detailData.createdAt}
              </Descriptions.Item>
              <Descriptions.Item label="更新时间">
                {detailData.updatedAt}
              </Descriptions.Item>
            </Descriptions>
            <Divider>申请材料</Divider>
            {docsLoading ? (
              <div style={{ textAlign: "center", padding: 20 }}>
                <Empty description="加载材料中..." />
              </div>
            ) : applicationDocs.length > 0 ? (
              <List
                dataSource={applicationDocs}
                renderItem={(doc) => (
                  <List.Item>
                    <Space style={{ width: "100%", justifyContent: "space-between" }}>
                      <Space>
                        <FileTextOutlined />
                        <span>{DOC_TYPE_MAP[doc.docType] || doc.docType || "未知类型"}</span>
                      </Space>
                      <Button
                        size="small"
                        icon={<EyeOutlined />}
                        onClick={() => handlePreview(doc.url)}
                      >
                        查看
                      </Button>
                    </Space>
                  </List.Item>
                )}
              />
            ) : (
              <Empty description="暂无申请材料" />
            )}
          </>
        ) : (
          <Empty description="暂无数据" />
        )}
      </Modal>
      <Modal
        title="预览材料"
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        footer={[
          <Button key="close" onClick={() => setPreviewVisible(false)}>
            关闭
          </Button>,
        ]}
        width={800}
      >
        {previewUrl && (
          <div style={{ textAlign: "center" }}>
            {previewUrl.toLowerCase().endsWith(".pdf") ? (
              <iframe
                src={previewUrl}
                style={{ width: "100%", height: "600px", border: "none" }}
                title="PDF预览"
              />
            ) : (
              <img
                src={previewUrl}
                alt="预览"
                style={{ maxWidth: "100%", maxHeight: "600px" }}
              />
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}

