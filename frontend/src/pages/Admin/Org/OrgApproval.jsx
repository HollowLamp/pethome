import React, { useEffect, useMemo, useState } from "react";
import {
  Card,
  Button,
  message,
  Typography,
  Space,
  Table,
  Tag,
  Modal,
  Input,
  Empty,
} from "antd";
import {
  CheckOutlined,
  CloseOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import api from "../../../api";

const { Title, Paragraph } = Typography;

export default function OrgApproval() {
  const [pending, setPending] = useState([]);
  const [loading, setLoading] = useState(false);
  const [reasonVisible, setReasonVisible] = useState(false);
  const [reason, setReason] = useState("");
  const [currentOrg, setCurrentOrg] = useState(null);
  const [actionType, setActionType] = useState("approve");
  const [submitting, setSubmitting] = useState(false);

  const fetchPending = async () => {
    setLoading(true);
    try {
      const res = await api.org.getPendingOrganizations();
      if (res?.code === 200) {
        const list = Array.isArray(res.data) ? res.data : res.data?.list || [];
        setPending(list);
      }
    } catch (e) {
      message.error(e?.message || "获取待审核机构失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPending();
  }, []);

  const openReasonModal = (record, type) => {
    setCurrentOrg(record);
    setActionType(type);
    setReason("");
    setReasonVisible(true);
  };

  const handleSubmit = async () => {
    if (!currentOrg) return;
    const orgId = currentOrg.id || currentOrg.organizationId;
    if (!orgId) {
      message.error("缺少机构ID");
      return;
    }
    setSubmitting(true);
    try {
      const payload = { reason: reason.trim() || undefined };
      const apiFn =
        actionType === "approve" ? api.org.approveOrg : api.org.rejectOrg;
      const res = await apiFn(orgId, payload);
      if (res?.code === 200) {
        message.success(actionType === "approve" ? "已审核通过" : "已拒绝申请");
        setReasonVisible(false);
        fetchPending();
      } else {
        message.error(res?.message || "操作失败");
      }
    } catch (e) {
      message.error(e?.message || "操作失败");
    } finally {
      setSubmitting(false);
    }
  };

  const columns = useMemo(
    () => [
      {
        title: "机构ID",
        dataIndex: "id",
        key: "id",
        render: (value, record) => value || record.organizationId,
      },
      {
        title: "机构名称",
        dataIndex: "name",
        key: "name",
      },
      {
        title: "联系人",
        dataIndex: "contactName",
        key: "contactName",
      },
      {
        title: "联系电话",
        dataIndex: "contactPhone",
        key: "contactPhone",
      },
      {
        title: "资质链接",
        dataIndex: "licenseUrl",
        key: "licenseUrl",
        render: (value) =>
          value ? (
            <a href={value} target="_blank" rel="noreferrer">
              查看资质
            </a>
          ) : (
            <Tag color="red">未上传</Tag>
          ),
      },
      {
        title: "提交时间",
        dataIndex: "createdAt",
        key: "createdAt",
        render: (value, record) => value || record.orgCreatedAt || "-",
      },
      {
        title: "操作",
        key: "action",
        width: 200,
        render: (_, record) => (
          <Space>
            <Button
              type="primary"
              icon={<CheckOutlined />}
              onClick={() => openReasonModal(record, "approve")}
            >
              通过
            </Button>
            <Button
              danger
              icon={<CloseOutlined />}
              onClick={() => openReasonModal(record, "reject")}
            >
              拒绝
            </Button>
          </Space>
        ),
      },
    ],
    []
  );

  return (
    <div>
      <Card style={{ marginBottom: 24 }}>
        <Title level={3}>机构入驻审核</Title>
        <Paragraph type="secondary">
          审核员可根据机构提交的资质信息进行审核。通过后机构将进入“启用”状态，并自动将申请人加入机构成员列表；若审核拒绝，可填写备注说明具体原因。
        </Paragraph>
      </Card>

      <Card
        title="待审核机构"
        extra={
          <Button
            icon={<ReloadOutlined />}
            onClick={fetchPending}
            loading={loading}
          >
            刷新
          </Button>
        }
      >
        <Table
          rowKey={(record) => record.id || record.organizationId}
          columns={columns}
          dataSource={pending}
          loading={loading}
          locale={{ emptyText: <Empty description="暂无待审核机构" /> }}
        />
      </Card>

      <Modal
        title={actionType === "approve" ? "审核通过" : "拒绝申请"}
        open={reasonVisible}
        onCancel={() => setReasonVisible(false)}
        onOk={handleSubmit}
        okText={actionType === "approve" ? "确认通过" : "确认拒绝"}
        okButtonProps={{ loading: submitting, danger: actionType === "reject" }}
      >
        <Input.TextArea
          rows={4}
          maxLength={200}
          placeholder="可选，填写审批备注"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          showCount
        />
      </Modal>
    </div>
  );
}
