import React, { useEffect, useMemo, useState } from "react";
import {
  Card,
  Table,
  Tag,
  Space,
  Button,
  Empty,
  Descriptions,
  Modal,
  Typography,
  Tabs,
  App as AntdApp,
  Radio,
  Spin,
  Form,
  InputNumber,
  Input,
  Select,
  Upload,
  Image,
} from "antd";
import {
  EyeOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CalendarOutlined,
  MedicineBoxOutlined,
  MessageOutlined,
  PlusOutlined,
  DeleteOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router";
import api from "../../api";
import useAuthStore from "../../store/authStore";

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

export default function AdoptionRecord() {
  const { message } = AntdApp.useApp();
  const { isLoggedIn } = useAuthStore();
  const navigate = useNavigate();
  const [applications, setApplications] = useState([]);
  const [adoptedPets, setAdoptedPets] = useState([]);
  const [loading, setLoading] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailData, setDetailData] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [activeTab, setActiveTab] = useState("all");
  const [interviewData, setInterviewData] = useState(null);
  const [interviewLoading, setInterviewLoading] = useState(false);
  const [interviewModalVisible, setInterviewModalVisible] = useState(false);
  const [currentAppForInterview, setCurrentAppForInterview] = useState(null);
  const [availableSlots, setAvailableSlots] = useState([]);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [selectedSlotId, setSelectedSlotId] = useState(null);
  const [submittingInterview, setSubmittingInterview] = useState(false);
  const [healthModalVisible, setHealthModalVisible] = useState(false);
  const [currentPetForHealth, setCurrentPetForHealth] = useState(null);
  const [healthForm] = Form.useForm();
  const [submittingHealth, setSubmittingHealth] = useState(false);
  const [healthLoading, setHealthLoading] = useState(false);
  const [currentHealth, setCurrentHealth] = useState(null);
  const [feedbackModalVisible, setFeedbackModalVisible] = useState(false);
  const [currentPetForFeedback, setCurrentPetForFeedback] = useState(null);
  const [feedbackForm] = Form.useForm();
  const [submittingFeedback, setSubmittingFeedback] = useState(false);
  const [feedbackFileList, setFeedbackFileList] = useState([]);
  const [feedbacks, setFeedbacks] = useState([]);
  const [feedbackLoading, setFeedbackLoading] = useState(false);
  const [viewFeedbackModalVisible, setViewFeedbackModalVisible] = useState(false);

  const fetchApplications = async () => {
    if (!isLoggedIn) {
      setApplications([]);
      return;
    }
    setLoading(true);
    try {
      const res = await api.adoption.getMyApplications();
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

  const fetchAdoptedPets = async () => {
    if (!isLoggedIn) {
      setAdoptedPets([]);
      return;
    }
    setLoading(true);
    try {
      const res = await api.adoption.getAdoptedPets();
      if (res?.code === 200) {
        const list = Array.isArray(res.data) ? res.data : res.data?.list || [];
        setAdoptedPets(list);
      }
    } catch (e) {
      message.error(e?.message || "获取已领养宠物列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isLoggedIn) {
      fetchApplications();
      fetchAdoptedPets();
    } else {
      setApplications([]);
      setAdoptedPets([]);
    }
  }, [isLoggedIn]);

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

  const handleRequestInterview = async (record) => {
    setCurrentAppForInterview(record);
    setSelectedSlotId(null);
    setAvailableSlots([]);
    setInterviewModalVisible(true);
    setSlotsLoading(true);
    try {
      const res = await api.interview.getAvailableSlots(record.id);
      if (res?.code === 200) {
        const list = Array.isArray(res.data) ? res.data : res.data?.list || [];
        setAvailableSlots(list);
        if (list.length === 0) {
          message.warning("暂无可用时段，请联系机构管理员");
        }
      } else {
        message.error(res?.message || "获取可用时段失败");
      }
    } catch (e) {
      message.error(e?.message || "获取可用时段失败");
    } finally {
      setSlotsLoading(false);
    }
  };

  const handleSubmitInterview = async () => {
    if (!currentAppForInterview || !selectedSlotId) {
      message.error("请选择面谈时段");
      return;
    }
    setSubmittingInterview(true);
    try {
      const res = await api.interview.requestInterview(
        currentAppForInterview.id,
        selectedSlotId
      );
      if (res?.code === 200) {
        message.success("面谈预约已提交，等待机构确认");
        setInterviewModalVisible(false);
        setCurrentAppForInterview(null);
        setSelectedSlotId(null);
        fetchApplications();
      } else {
        message.error(res?.message || "预约失败");
      }
    } catch (e) {
      message.error(e?.message || "预约失败");
    } finally {
      setSubmittingInterview(false);
    }
  };

  const formatDateTime = (dateTime) => {
    if (!dateTime) return "-";
    return new Date(dateTime).toLocaleString("zh-CN");
  };

  // 面谈状态映射
  const INTERVIEW_STATUS_MAP = {
    REQUESTED: { label: "待确认", color: "orange" },
    CONFIRMED: { label: "已确认", color: "blue" },
    CANCELED: { label: "已取消", color: "red" },
    DONE: { label: "已完成", color: "green" },
  };

  const handleViewPet = (petId) => {
    navigate(`/pets/${petId}`);
  };

  const handleUpdateHealth = async (record) => {
    setCurrentPetForHealth(record);
    setHealthModalVisible(true);
    setHealthLoading(true);
    setCurrentHealth(null);
    healthForm.resetFields();

    try {
      const res = await api.pets.getPetHealth(record.petId);
      if (res?.code === 200) {
        setCurrentHealth(res.data);
        const vaccine = res.data?.vaccine
          ? JSON.parse(res.data.vaccine)
          : [];
        healthForm.setFieldsValue({
          weight: res.data?.weight ? Number(res.data.weight) : undefined,
          vaccine: vaccine,
          note: res.data?.note || "",
        });
      } else if (res?.code === 404) {
        // 暂无健康记录，使用空表单
        setCurrentHealth(null);
      }
    } catch (e) {
      if (e?.code !== 404) {
        message.error(e?.message || "获取健康信息失败");
      }
    } finally {
      setHealthLoading(false);
    }
  };

  const handleSubmitHealth = async () => {
    if (!currentPetForHealth) {
      return;
    }

    try {
      const values = await healthForm.validateFields();
      setSubmittingHealth(true);

      const payload = {
        weight: values.weight,
        vaccine: JSON.stringify(values.vaccine || []),
        note: values.note || "",
      };

      const res = await api.pets.updatePetHealthByOwner(
        currentPetForHealth.petId,
        payload
      );

      if (res?.code === 200) {
        message.success("健康状态更新成功");
        setHealthModalVisible(false);
        setCurrentPetForHealth(null);
        healthForm.resetFields();
      } else {
        message.error(res?.message || "更新失败");
      }
    } catch (e) {
      if (!e?.errorFields) {
        message.error(e?.message || "更新失败");
      }
    } finally {
      setSubmittingHealth(false);
    }
  };

  // 打开反馈上传模态框
  const handleUploadFeedback = (record) => {
    setCurrentPetForFeedback(record);
    setFeedbackModalVisible(true);
    feedbackForm.resetFields();
    setFeedbackFileList([]);
  };

  // 提交反馈
  const handleSubmitFeedback = async () => {
    if (!currentPetForFeedback) {
      return;
    }

    try {
      const values = await feedbackForm.validateFields();
      setSubmittingFeedback(true);

      // 获取上传的文件
      const files = feedbackFileList
        .filter((file) => file.originFileObj)
        .map((file) => file.originFileObj);

      const res = await api.pets.createPetFeedback(
        currentPetForFeedback.petId,
        {
          content: values.content || "",
          files: files,
        }
      );

      if (res?.code === 200) {
        message.success("反馈提交成功");
        // 清理预览URL
        feedbackFileList.forEach((file) => {
          if (file.preview) {
            URL.revokeObjectURL(file.preview);
          }
        });
        setFeedbackModalVisible(false);
        setCurrentPetForFeedback(null);
        feedbackForm.resetFields();
        setFeedbackFileList([]);
        // 刷新已领养宠物列表
        fetchAdoptedPets();
      } else {
        message.error(res?.message || "提交失败");
      }
    } catch (e) {
      if (!e?.errorFields) {
        message.error(e?.message || "提交失败");
      }
    } finally {
      setSubmittingFeedback(false);
    }
  };

  // 查看反馈
  const handleViewFeedback = async (record) => {
    setCurrentPetForFeedback(record);
    setViewFeedbackModalVisible(true);
    setFeedbackLoading(true);
    try {
      const res = await api.pets.getPetFeedbacks(record.petId);
      if (res?.code === 200) {
        setFeedbacks(res.data || []);
      } else {
        message.error(res?.message || "获取反馈失败");
        setFeedbacks([]);
      }
    } catch (e) {
      message.error(e?.message || "获取反馈失败");
      setFeedbacks([]);
    } finally {
      setFeedbackLoading(false);
    }
  };

  // 解析媒体URL
  const parseMediaUrls = (mediaUrlsStr) => {
    if (!mediaUrlsStr) return [];
    try {
      const parsed = JSON.parse(mediaUrlsStr);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  };

  const getFilteredApplications = () => {
    if (activeTab === "all") return applications;
    if (activeTab === "pending") {
      return applications.filter(
        (app) =>
          app.status === "PENDING" || app.status === "ORG_APPROVED" || app.status === "PLATFORM_APPROVED"
      );
    }
    if (activeTab === "completed") {
      return applications.filter((app) => app.status === "COMPLETED");
    }
    if (activeTab === "rejected") {
      return applications.filter(
        (app) => app.status === "ORG_REJECTED" || app.status === "PLATFORM_REJECTED"
      );
    }
    return applications;
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
        render: (value) => (
          <Button
            type="link"
            size="small"
            onClick={() => handleViewPet(value)}
            style={{ padding: 0 }}
          >
            {value}
          </Button>
        ),
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
        width: 200,
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
            {record.status === "PLATFORM_APPROVED" && (
              <Button
                type="primary"
                size="small"
                icon={<CalendarOutlined />}
                onClick={() => handleRequestInterview(record)}
              >
                预约面谈
              </Button>
            )}
          </Space>
        ),
      },
    ],
    []
  );

  const adoptedColumns = useMemo(
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
        render: (value) => (
          <Button
            type="link"
            size="small"
            onClick={() => handleViewPet(value)}
            style={{ padding: 0 }}
          >
            {value}
          </Button>
        ),
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
        title: "完成时间",
        dataIndex: "updatedAt",
        key: "updatedAt",
        width: 180,
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
              icon={<EyeOutlined />}
              onClick={() => handleViewDetail(record)}
            >
              查看详情
            </Button>
            <Button
              type="primary"
              size="small"
              icon={<MedicineBoxOutlined />}
              onClick={() => handleUpdateHealth(record)}
            >
              更新健康状态
            </Button>
            <Button
              type="default"
              size="small"
              icon={<MessageOutlined />}
              onClick={() => handleUploadFeedback(record)}
            >
              上传反馈
            </Button>
            <Button
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleViewFeedback(record)}
            >
              查看反馈
            </Button>
          </Space>
        ),
      },
    ],
    []
  );

  if (!isLoggedIn) {
    return (
      <div style={{ padding: 24, textAlign: "center" }}>
        <Card>
          <Empty
            description="请先登录后查看领养记录"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button
              type="primary"
              onClick={() => window.dispatchEvent(new Event("OPEN_LOGIN_MODAL"))}
            >
              去登录
            </Button>
          </Empty>
        </Card>
      </div>
    );
  }

  return (
    <div style={{ padding: 24 }}>
      <Card style={{ marginBottom: 24 }}>
        <Title level={2}>领养记录</Title>
        <Paragraph type="secondary">
          查看您的领养申请状态和已领养的宠物信息
        </Paragraph>
      </Card>

      <Card
        title="我的申请"
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
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: "all",
              label: `全部 (${applications.length})`,
              children: (
                <Table
                  rowKey="id"
                  columns={columns}
                  dataSource={getFilteredApplications()}
                  loading={loading}
                  locale={{ emptyText: <Empty description="暂无申请记录" /> }}
                  scroll={{ x: 1000 }}
                  pagination={{
                    pageSize: 10,
                    showSizeChanger: true,
                    showTotal: (total) => `共 ${total} 条记录`,
                  }}
                />
              ),
            },
            {
              key: "pending",
              label: `进行中 (${applications.filter((app) => app.status === "PENDING" || app.status === "ORG_APPROVED" || app.status === "PLATFORM_APPROVED").length})`,
              children: (
                <Table
                  rowKey="id"
                  columns={columns}
                  dataSource={getFilteredApplications()}
                  loading={loading}
                  locale={{ emptyText: <Empty description="暂无申请记录" /> }}
                  scroll={{ x: 1000 }}
                  pagination={{
                    pageSize: 10,
                    showSizeChanger: true,
                    showTotal: (total) => `共 ${total} 条记录`,
                  }}
                />
              ),
            },
            {
              key: "completed",
              label: `已完成 (${applications.filter((app) => app.status === "COMPLETED").length})`,
              children: (
                <Table
                  rowKey="id"
                  columns={columns}
                  dataSource={getFilteredApplications()}
                  loading={loading}
                  locale={{ emptyText: <Empty description="暂无申请记录" /> }}
                  scroll={{ x: 1000 }}
                  pagination={{
                    pageSize: 10,
                    showSizeChanger: true,
                    showTotal: (total) => `共 ${total} 条记录`,
                  }}
                />
              ),
            },
            {
              key: "rejected",
              label: `已拒绝 (${applications.filter((app) => app.status === "ORG_REJECTED" || app.status === "PLATFORM_REJECTED").length})`,
              children: (
                <Table
                  rowKey="id"
                  columns={columns}
                  dataSource={getFilteredApplications()}
                  loading={loading}
                  locale={{ emptyText: <Empty description="暂无申请记录" /> }}
                  scroll={{ x: 1000 }}
                  pagination={{
                    pageSize: 10,
                    showSizeChanger: true,
                    showTotal: (total) => `共 ${total} 条记录`,
                  }}
                />
              ),
            },
          ]}
        />
      </Card>

      <Card
        title="已领养宠物"
        extra={
          <Button
            icon={<ReloadOutlined />}
            onClick={fetchAdoptedPets}
            loading={loading}
          >
            刷新
          </Button>
        }
        style={{ marginTop: 24 }}
      >
        <Table
          rowKey="id"
          columns={adoptedColumns}
          dataSource={adoptedPets}
          loading={loading}
          locale={{ emptyText: <Empty description="暂无已领养宠物" /> }}
          scroll={{ x: 800 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
        />
      </Card>

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
          detailData?.petId && (
            <Button
              key="viewPet"
              type="primary"
              onClick={() => {
                setDetailVisible(false);
                handleViewPet(detailData.petId);
              }}
            >
              查看宠物详情
            </Button>
          ),
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
              <Descriptions.Item label="宠物ID">
                <Button
                  type="link"
                  onClick={() => {
                    setDetailVisible(false);
                    handleViewPet(detailData.petId);
                  }}
                  style={{ padding: 0 }}
                >
                  {detailData.petId}
                </Button>
              </Descriptions.Item>
              <Descriptions.Item label="机构ID">{detailData.orgId}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={STATUS_MAP[detailData.status]?.color || "default"}>
                  {STATUS_MAP[detailData.status]?.label || detailData.status}
                </Tag>
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
                    <Tag
                      color={
                        INTERVIEW_STATUS_MAP[interviewData.status]?.color ||
                        "default"
                      }
                    >
                      {INTERVIEW_STATUS_MAP[interviewData.status]?.label ||
                        interviewData.status}
                    </Tag>
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
            ) : detailData.status === "PLATFORM_APPROVED" ? (
              <Paragraph style={{ marginTop: 24, textAlign: "center" }}>
                暂无面谈预约，请点击"预约面谈"按钮进行预约
              </Paragraph>
            ) : null}
          </>
        ) : (
          <Empty description="暂无数据" />
        )}
      </Modal>

      {/* 预约面谈模态框 */}
      <Modal
        title="预约面谈"
        open={interviewModalVisible}
        onCancel={() => {
          setInterviewModalVisible(false);
          setCurrentAppForInterview(null);
          setSelectedSlotId(null);
          setAvailableSlots([]);
        }}
        onOk={handleSubmitInterview}
        okText="提交预约"
        okButtonProps={{ loading: submittingInterview, disabled: !selectedSlotId }}
        width={700}
      >
        {currentAppForInterview && (
          <div>
            <Paragraph>
              <strong>申请信息</strong>
            </Paragraph>
            <Descriptions column={1} bordered size="small" style={{ marginBottom: 24 }}>
              <Descriptions.Item label="申请ID">
                {currentAppForInterview.id}
              </Descriptions.Item>
              <Descriptions.Item label="宠物ID">
                {currentAppForInterview.petId}
              </Descriptions.Item>
            </Descriptions>
            <Paragraph>
              <strong>选择面谈时段</strong>
            </Paragraph>
            {slotsLoading ? (
              <div style={{ textAlign: "center", padding: 40 }}>
                <Spin size="large" />
                <Paragraph type="secondary" style={{ marginTop: 16 }}>
                  加载可用时段中...
                </Paragraph>
              </div>
            ) : availableSlots.length === 0 ? (
              <Empty
                description="暂无可用时段"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
              >
                <Paragraph type="secondary">
                  请联系机构管理员设置可用时段，或稍后再试。
                </Paragraph>
              </Empty>
            ) : (
              <Radio.Group
                value={selectedSlotId}
                onChange={(e) => setSelectedSlotId(e.target.value)}
                style={{ width: "100%" }}
              >
                <Space direction="vertical" style={{ width: "100%" }}>
                  {availableSlots.map((slot) => (
                    <Radio key={slot.id} value={slot.id}>
                      <Space>
                        <span>
                          {formatDateTime(slot.startAt)} - {formatDateTime(slot.endAt)}
                        </span>
                        <Tag color="green">可预约</Tag>
                      </Space>
                    </Radio>
                  ))}
                </Space>
              </Radio.Group>
            )}
          </div>
        )}
      </Modal>

      {/* 更新健康状态模态框 */}
      <Modal
        title="更新宠物健康状态"
        open={healthModalVisible}
        onCancel={() => {
          setHealthModalVisible(false);
          setCurrentPetForHealth(null);
          setCurrentHealth(null);
          healthForm.resetFields();
        }}
        onOk={handleSubmitHealth}
        okText="提交"
        okButtonProps={{ loading: submittingHealth }}
        width={600}
      >
        {healthLoading ? (
          <div style={{ textAlign: "center", padding: 40 }}>
            <Spin size="large" />
          </div>
        ) : currentPetForHealth ? (
          <Form form={healthForm} layout="vertical">
            <Form.Item label="宠物ID">
              <Input value={currentPetForHealth.petId} disabled />
            </Form.Item>
            <Form.Item
              name="weight"
              label="体重 (kg)"
              rules={[{ required: true, message: "请输入体重" }]}
            >
              <InputNumber
                min={0}
                step={0.1}
                style={{ width: "100%" }}
                placeholder="例如：5.2"
              />
            </Form.Item>
            <Form.Item name="vaccine" label="疫苗接种">
              <Select
                mode="tags"
                tokenSeparators={[","]}
                placeholder="请填写已接种疫苗，回车或逗号分隔"
                allowClear
              />
            </Form.Item>
            <Form.Item name="note" label="健康备注">
              <Input.TextArea
                rows={4}
                maxLength={400}
                placeholder="补充健康状况、喂养注意事项等"
                showCount
              />
            </Form.Item>
          </Form>
        ) : (
          <Empty description="加载中..." />
        )}
      </Modal>

      {/* 上传反馈模态框 */}
      <Modal
        title="上传宠物反馈"
        open={feedbackModalVisible}
        onCancel={() => {
          // 清理预览URL
          feedbackFileList.forEach((file) => {
            if (file.preview) {
              URL.revokeObjectURL(file.preview);
            }
          });
          setFeedbackModalVisible(false);
          setCurrentPetForFeedback(null);
          feedbackForm.resetFields();
          setFeedbackFileList([]);
        }}
        onOk={handleSubmitFeedback}
        okText="提交反馈"
        okButtonProps={{ loading: submittingFeedback }}
        width={700}
      >
        {currentPetForFeedback && (
          <Form form={feedbackForm} layout="vertical">
            <Form.Item label="宠物ID">
              <Input value={currentPetForFeedback.petId} disabled />
            </Form.Item>
            <Form.Item
              name="content"
              label="反馈内容"
              rules={[
                {
                  max: 500,
                  message: "反馈内容不能超过500字",
                },
              ]}
            >
              <Input.TextArea
                rows={6}
                maxLength={500}
                placeholder="分享您与宠物的日常，记录成长点滴..."
                showCount
              />
            </Form.Item>
            <Form.Item label="上传图片/视频">
              <Upload
                listType="picture-card"
                fileList={feedbackFileList}
                onChange={({ fileList }) => {
                  // 为每个文件生成预览URL
                  const newFileList = fileList.map((file) => {
                    if (file.originFileObj) {
                      file.preview = URL.createObjectURL(file.originFileObj);
                    }
                    return file;
                  });
                  setFeedbackFileList(newFileList);
                }}
                onRemove={(file) => {
                  // 清理预览URL
                  if (file.preview) {
                    URL.revokeObjectURL(file.preview);
                  }
                  const index = feedbackFileList.indexOf(file);
                  const newFileList = feedbackFileList.slice();
                  newFileList.splice(index, 1);
                  setFeedbackFileList(newFileList);
                }}
                beforeUpload={(file) => {
                  // 检查文件类型
                  const isImage = file.type.startsWith("image/");
                  const isVideo = file.type.startsWith("video/");
                  if (!isImage && !isVideo) {
                    message.error("只支持上传图片或视频文件");
                    return false;
                  }
                  // 检查文件大小（限制为50MB）
                  const isLt50M = file.size / 1024 / 1024 < 50;
                  if (!isLt50M) {
                    message.error("文件大小不能超过50MB");
                    return false;
                  }
                  // 不自动上传，等待表单提交时一起上传
                  return false;
                }}
                accept="image/*,video/*"
                multiple
              >
                {feedbackFileList.length >= 9 ? null : (
                  <div>
                    <PlusOutlined />
                    <div style={{ marginTop: 8 }}>上传</div>
                  </div>
                )}
              </Upload>
              <Text type="secondary" style={{ fontSize: 12 }}>
                支持上传图片或视频，最多9个文件，单个文件不超过50MB
              </Text>
            </Form.Item>
          </Form>
        )}
      </Modal>

      {/* 查看反馈模态框 */}
      <Modal
        title={`宠物反馈 - 宠物ID: ${currentPetForFeedback?.petId || ""}`}
        open={viewFeedbackModalVisible}
        onCancel={() => {
          setViewFeedbackModalVisible(false);
          setCurrentPetForFeedback(null);
          setFeedbacks([]);
        }}
        footer={[
          <Button key="close" onClick={() => setViewFeedbackModalVisible(false)}>
            关闭
          </Button>,
        ]}
        width={800}
      >
        {feedbackLoading ? (
          <div style={{ textAlign: "center", padding: 40 }}>
            <Spin size="large" />
          </div>
        ) : feedbacks.length > 0 ? (
          <div style={{ maxHeight: 600, overflowY: "auto" }}>
            {feedbacks.map((feedback) => {
              const mediaUrls = parseMediaUrls(feedback.mediaUrls);
              const createdAt = feedback.createdAt
                ? new Date(feedback.createdAt).toLocaleString("zh-CN")
                : "--";
              return (
                <Card
                  key={feedback.id}
                  style={{ marginBottom: 16 }}
                  size="small"
                >
                  <Space direction="vertical" style={{ width: "100%" }} size={8}>
                    <Space split={<Tag color="default">|</Tag>} wrap>
                      <Text type="secondary">反馈ID: {feedback.id}</Text>
                      <Text type="secondary">用户ID: {feedback.userId}</Text>
                      <Text type="secondary">时间: {createdAt}</Text>
                    </Space>
                    {feedback.content && (
                      <Paragraph style={{ marginBottom: 8 }}>
                        {feedback.content}
                      </Paragraph>
                    )}
                    {mediaUrls.length > 0 && (
                      <div>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          附件 ({mediaUrls.length}):
                        </Text>
                        <div
                          style={{
                            display: "flex",
                            flexWrap: "wrap",
                            gap: 8,
                            marginTop: 8,
                          }}
                        >
                          {mediaUrls.map((url, index) => {
                            const isImage = /\.(jpg|jpeg|png|gif|webp)$/i.test(
                              url
                            );
                            return (
                              <div key={index} style={{ position: "relative" }}>
                                {isImage ? (
                                  <Image
                                    src={url}
                                    alt={`反馈图片-${index + 1}`}
                                    width={120}
                                    height={120}
                                    style={{
                                      objectFit: "cover",
                                      borderRadius: 4,
                                    }}
                                    preview={{
                                      mask: "预览",
                                    }}
                                  />
                                ) : (
                                  <div
                                    style={{
                                      width: 120,
                                      height: 120,
                                      border: "1px solid #d9d9d9",
                                      borderRadius: 4,
                                      display: "flex",
                                      alignItems: "center",
                                      justifyContent: "center",
                                      backgroundColor: "#fafafa",
                                    }}
                                  >
                                    <a
                                      href={url}
                                      target="_blank"
                                      rel="noreferrer"
                                    >
                                      <Button size="small" type="link">
                                        查看视频
                                      </Button>
                                    </a>
                                  </div>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    )}
                    {!feedback.content && mediaUrls.length === 0 && (
                      <Text type="secondary">（无内容）</Text>
                    )}
                  </Space>
                </Card>
              );
            })}
          </div>
        ) : (
          <Empty description="暂无反馈记录" />
        )}
      </Modal>
    </div>
  );
}

