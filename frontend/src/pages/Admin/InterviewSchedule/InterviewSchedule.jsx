import React, { useEffect, useMemo, useState } from "react";
import {
  Card,
  Button,
  Typography,
  Space,
  Table,
  Tag,
  Modal,
  Empty,
  Descriptions,
  App as AntdApp,
  Form,
  DatePicker,
  Switch,
  Tabs,
  Popconfirm,
} from "antd";
import {
  CheckOutlined,
  ReloadOutlined,
  EyeOutlined,
  CalendarOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
} from "@ant-design/icons";
import dayjs from "dayjs";
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

// 面谈预约状态映射
const INTERVIEW_STATUS_MAP = {
  REQUESTED: { label: "待确认", color: "orange" },
  CONFIRMED: { label: "已确认", color: "blue" },
  CANCELED: { label: "已取消", color: "red" },
  DONE: { label: "已完成", color: "green" },
};

export default function InterviewSchedule() {
  const { message, modal } = AntdApp.useApp();
  const { user } = useAuthStore();
  const userId = user?.id || user?.userId;
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [confirmVisible, setConfirmVisible] = useState(false);
  const [completeVisible, setCompleteVisible] = useState(false);
  const [currentApp, setCurrentApp] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailData, setDetailData] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [interviewData, setInterviewData] = useState(null);
  const [interviewLoading, setInterviewLoading] = useState(false);
  const [slots, setSlots] = useState([]);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [slotModalVisible, setSlotModalVisible] = useState(false);
  const [slotForm] = Form.useForm();
  const [editingSlot, setEditingSlot] = useState(null);
  const [activeTab, setActiveTab] = useState("applications");
  const [currentOrg, setCurrentOrg] = useState(null);
  const [orgLoading, setOrgLoading] = useState(false);

  const fetchApplications = async () => {
    setLoading(true);
    try {
      // 获取平台已通过的申请（状态为 PLATFORM_APPROVED），可以安排面谈
      const res = await api.adoption.getPendingApplications("PLATFORM_APPROVED");
      if (res?.code === 200) {
        const list = Array.isArray(res.data) ? res.data : res.data?.list || [];
        // 为每个申请获取面谈信息
        const appsWithInterview = await Promise.all(
          list.map(async (app) => {
            try {
              const interviewRes = await api.interview.getInterviewRequest(app.id);
              if (interviewRes?.code === 200) {
                return { ...app, interview: interviewRes.data };
              }
            } catch (e) {
              // 如果没有面谈预约，interview 为 null
            }
            return { ...app, interview: null };
          })
        );
        setApplications(appsWithInterview);
      }
    } catch (e) {
      message.error(e?.message || "获取申请列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchApplications();
    fetchSlots();
    fetchCurrentOrg();
  }, []);

  const fetchCurrentOrg = async () => {
    if (!userId) {
      setCurrentOrg(null);
      return;
    }
    setOrgLoading(true);
    try {
      const res = await api.org.getUserMemberships(userId);
      if (res?.code === 200) {
        const list = Array.isArray(res.data)
          ? res.data
          : res.data?.list || res.data?.memberships || [];
        if (list.length > 0) {
          const firstOrg = list[0].organizationId ? list[0] : list[0].org || list[0];
          setCurrentOrg({
            id: firstOrg.orgId || firstOrg.organizationId || firstOrg.id,
            name: firstOrg.name || `机构 ${firstOrg.orgId || firstOrg.organizationId || firstOrg.id}`,
          });
        } else {
          setCurrentOrg(null);
        }
      }
    } catch (e) {
      console.warn("获取机构信息失败", e);
      setCurrentOrg(null);
    } finally {
      setOrgLoading(false);
    }
  };

  const fetchSlots = async () => {
    setSlotsLoading(true);
    try {
      const res = await api.interview.getScheduleSlots();
      if (res?.code === 200) {
        const list = Array.isArray(res.data) ? res.data : res.data?.list || [];
        setSlots(list);
      }
    } catch (e) {
      message.error(e?.message || "获取时段列表失败");
    } finally {
      setSlotsLoading(false);
    }
  };

  const handleCreateSlot = () => {
    setEditingSlot(null);
    slotForm.resetFields();
    setSlotModalVisible(true);
  };

  const handleEditSlot = (slot) => {
    setEditingSlot(slot);
    slotForm.setFieldsValue({
      startAt: slot.startAt ? dayjs(slot.startAt) : null,
      endAt: slot.endAt ? dayjs(slot.endAt) : null,
      isOpen: slot.isOpen !== false,
    });
    setSlotModalVisible(true);
  };

  const handleDeleteSlot = async (slotId) => {
    try {
      const res = await api.interview.deleteScheduleSlot(slotId);
      if (res?.code === 200) {
        message.success("时段已删除");
        fetchSlots();
      } else {
        message.error(res?.message || "删除失败");
      }
    } catch (e) {
      message.error(e?.message || "删除失败");
    }
  };

  const handleSubmitSlot = async () => {
    try {
      const values = await slotForm.validateFields();
      const slotData = {
        startAt: values.startAt.format("YYYY-MM-DD HH:mm:ss"),
        endAt: values.endAt.format("YYYY-MM-DD HH:mm:ss"),
        isOpen: values.isOpen !== false,
      };
      if (editingSlot) {
        const res = await api.interview.updateScheduleSlot(editingSlot.id, slotData);
        if (res?.code === 200) {
          message.success("时段已更新");
          setSlotModalVisible(false);
          fetchSlots();
        } else {
          message.error(res?.message || "更新失败");
        }
      } else {
        const res = await api.interview.createScheduleSlot(slotData);
        if (res?.code === 200) {
          message.success("时段已创建");
          setSlotModalVisible(false);
          fetchSlots();
        } else {
          message.error(res?.message || "创建失败");
        }
      }
    } catch (e) {
      if (e.errorFields) {
        return;
      }
      message.error(e?.message || "操作失败");
    }
  };

  const openConfirmModal = (record) => {
    setCurrentApp(record);
    setConfirmVisible(true);
  };

  const openCompleteModal = (record) => {
    setCurrentApp(record);
    setCompleteVisible(true);
  };

  const handleConfirmInterview = async () => {
    if (!currentApp) return;
    const appId = currentApp.id;
    if (!appId) {
      message.error("缺少申请ID");
      return;
    }
    setSubmitting(true);
    try {
      const res = await api.interview.confirmInterview(appId);
      if (res?.code === 200) {
        message.success("面谈已确认");
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

  const handleCompleteInterview = async () => {
    if (!currentApp) return;
    const appId = currentApp.id;
    if (!appId) {
      message.error("缺少申请ID");
      return;
    }
    setSubmitting(true);
    try {
      const res = await api.interview.completeInterview(appId);
      if (res?.code === 200) {
        message.success("面谈已完成");
        setCompleteVisible(false);
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
        title: "面谈状态",
        key: "interviewStatus",
        width: 120,
        render: (_, record) => {
          if (!record.interview) {
            return <Tag color="default">未预约</Tag>;
          }
          const status = INTERVIEW_STATUS_MAP[record.interview.status] || {
            label: record.interview.status,
            color: "default",
          };
          return <Tag color={status.color}>{status.label}</Tag>;
        },
      },
      {
        title: "面谈时间",
        key: "interviewTime",
        width: 200,
        render: (_, record) => {
          if (!record.interview?.slot) return "-";
          const slot = record.interview.slot;
          return `${formatDateTime(slot.startAt)} - ${formatDateTime(slot.endAt)}`;
        },
      },
      {
        title: "操作",
        key: "action",
        width: 300,
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
            {record.interview?.status === "REQUESTED" && (
              <Button
                type="primary"
                size="small"
                icon={<CheckOutlined />}
                onClick={() => openConfirmModal(record)}
              >
                确认面谈
              </Button>
            )}
            {record.interview?.status === "CONFIRMED" && (
              <Button
                type="primary"
                size="small"
                icon={<CalendarOutlined />}
                onClick={() => openCompleteModal(record)}
              >
                完成面谈
              </Button>
            )}
          </Space>
        ),
      },
    ],
    []
  );

  const slotColumns = useMemo(
    () => [
      {
        title: "时段ID",
        dataIndex: "id",
        key: "id",
        width: 100,
      },
      {
        title: "开始时间",
        dataIndex: "startAt",
        key: "startAt",
        width: 180,
        render: (value) => formatDateTime(value),
      },
      {
        title: "结束时间",
        dataIndex: "endAt",
        key: "endAt",
        width: 180,
        render: (value) => formatDateTime(value),
      },
      {
        title: "状态",
        dataIndex: "isOpen",
        key: "isOpen",
        width: 100,
        render: (value) => (
          <Tag color={value ? "green" : "red"}>
            {value ? "可预约" : "已关闭"}
          </Tag>
        ),
      },
      {
        title: "操作",
        key: "action",
        width: 200,
        fixed: "right",
        render: (_, record) => (
          <Space>
            <Button
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEditSlot(record)}
            >
              编辑
            </Button>
            <Popconfirm
              title="确定要删除此时段吗？"
              onConfirm={() => handleDeleteSlot(record.id)}
              okText="确定"
              cancelText="取消"
            >
              <Button
                size="small"
                danger
                icon={<DeleteOutlined />}
              >
                删除
              </Button>
            </Popconfirm>
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
              安排面谈时间
            </Title>
            <Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0 }}>
              机构管理员可管理面谈时段，查看用户提交的面谈预约请求，确认面谈时间。面谈完成后可进行交接确认。
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

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: "applications",
            label: "预约申请",
            children: (
              <Card
                title="待安排面谈申请"
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
                  locale={{ emptyText: <Empty description="暂无待安排面谈的申请" /> }}
                  scroll={{ x: 1200 }}
                />
              </Card>
            ),
          },
          {
            key: "slots",
            label: "时段管理",
            children: (
              <Card
                title="面谈时段管理"
                extra={
                  <Space>
                    <Button
                      icon={<ReloadOutlined />}
                      onClick={fetchSlots}
                      loading={slotsLoading}
                    >
                      刷新
                    </Button>
                    <Button
                      type="primary"
                      icon={<PlusOutlined />}
                      onClick={handleCreateSlot}
                    >
                      新增时段
                    </Button>
                  </Space>
                }
              >
                <Table
                  rowKey="id"
                  columns={slotColumns}
                  dataSource={slots}
                  loading={slotsLoading}
                  locale={{ emptyText: <Empty description="暂无时段，请创建" /> }}
                  scroll={{ x: 800 }}
                />
              </Card>
            ),
          },
        ]}
      />

      <Modal
        title="确认面谈"
        open={confirmVisible}
        onCancel={() => setConfirmVisible(false)}
        onOk={handleConfirmInterview}
        okText="确认"
        okButtonProps={{ loading: submitting, type: "primary" }}
      >
        <Paragraph>确认此面谈预约？确认后用户将收到通知。</Paragraph>
        {currentApp && currentApp.interview && (
          <Descriptions
            column={1}
            bordered
            size="small"
            style={{ marginTop: 16 }}
          >
            <Descriptions.Item label="申请ID">{currentApp.id}</Descriptions.Item>
            <Descriptions.Item label="面谈时间">
              {currentApp.interview.slot
                ? `${formatDateTime(currentApp.interview.slot.startAt)} - ${formatDateTime(currentApp.interview.slot.endAt)}`
                : "-"}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      <Modal
        title="完成面谈"
        open={completeVisible}
        onCancel={() => setCompleteVisible(false)}
        onOk={handleCompleteInterview}
        okText="确认完成"
        okButtonProps={{ loading: submitting, type: "primary" }}
      >
        <Paragraph>确认面谈已完成？完成后可进行交接确认。</Paragraph>
        {currentApp && currentApp.interview && (
          <Descriptions
            column={1}
            bordered
            size="small"
            style={{ marginTop: 16 }}
          >
            <Descriptions.Item label="申请ID">{currentApp.id}</Descriptions.Item>
            <Descriptions.Item label="面谈时间">
              {currentApp.interview.slot
                ? `${formatDateTime(currentApp.interview.slot.startAt)} - ${formatDateTime(currentApp.interview.slot.endAt)}`
                : "-"}
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
              <Descriptions.Item label="申请ID">{detailData.id}</Descriptions.Item>
              <Descriptions.Item label="宠物ID">{detailData.petId}</Descriptions.Item>
              <Descriptions.Item label="申请人ID">
                {detailData.applicantId}
              </Descriptions.Item>
              <Descriptions.Item label="机构ID">{detailData.orgId}</Descriptions.Item>
              <Descriptions.Item label="状态">
                {STATUS_MAP[detailData.status]?.label || detailData.status}
              </Descriptions.Item>
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
                    {INTERVIEW_STATUS_MAP[interviewData.status]?.label || interviewData.status}
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
                暂无面谈预约
              </Paragraph>
            )}
          </>
        ) : (
          <Empty description="暂无数据" />
        )}
      </Modal>

      {/* 时段管理模态框 */}
      <Modal
        title={editingSlot ? "编辑时段" : "新增时段"}
        open={slotModalVisible}
        onCancel={() => {
          setSlotModalVisible(false);
          setEditingSlot(null);
          slotForm.resetFields();
        }}
        onOk={handleSubmitSlot}
        okText={editingSlot ? "更新" : "创建"}
        width={600}
      >
        <Form form={slotForm} layout="vertical">
          <Form.Item
            name="startAt"
            label="开始时间"
            rules={[{ required: true, message: "请选择开始时间" }]}
          >
            <DatePicker
              showTime
              format="YYYY-MM-DD HH:mm:ss"
              style={{ width: "100%" }}
              placeholder="选择开始时间"
            />
          </Form.Item>
          <Form.Item
            name="endAt"
            label="结束时间"
            rules={[{ required: true, message: "请选择结束时间" }]}
          >
            <DatePicker
              showTime
              format="YYYY-MM-DD HH:mm:ss"
              style={{ width: "100%" }}
              placeholder="选择结束时间"
            />
          </Form.Item>
          <Form.Item
            name="isOpen"
            label="是否可预约"
            valuePropName="checked"
            initialValue={true}
          >
            <Switch checkedChildren="可预约" unCheckedChildren="已关闭" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

