import React, { useState, useEffect } from "react";
import {
  Card,
  Button,
  Table,
  Space,
  Tag,
  Modal,
  Form,
  Input,
  Select,
  InputNumber,
  Upload,
  message,
  Divider,
  Typography,
} from "antd";
import {
  PlusOutlined,
  ReloadOutlined,
  UploadOutlined,
  EditOutlined,
  PictureOutlined,
} from "@ant-design/icons";
import api from "../../../api";
import useAuthStore from "../../../store/authStore";

const { Title, Paragraph, Text } = Typography;
const { Option } = Select;

const PET_TYPE_OPTIONS = [
  { value: "DOG", label: "狗狗" },
  { value: "CAT", label: "猫咪" },
  { value: "RABBIT", label: "兔子" },
  { value: "BIRD", label: "小鸟" },
  { value: "OTHER", label: "其他" },
];

const PET_STATUS_OPTIONS = [
  { value: "AVAILABLE", label: "待领养" },
  { value: "RESERVED", label: "已预订" },
  { value: "ADOPTED", label: "已领养" },
  { value: "ARCHIVED", label: "已下架" },
];

const PET_GENDER_OPTIONS = [
  { value: "MALE", label: "公" },
  { value: "FEMALE", label: "母" },
];

const PET_SIZE_OPTIONS = [
  { value: "SMALL", label: "小型" },
  { value: "MEDIUM", label: "中型" },
  { value: "LARGE", label: "大型" },
];

const STATUS_COLOR_MAP = {
  AVAILABLE: "green",
  RESERVED: "gold",
  ADOPTED: "blue",
  ARCHIVED: "default",
};

const TYPE_COLOR_MAP = {
  DOG: "volcano",
  CAT: "purple",
  RABBIT: "pink",
  BIRD: "cyan",
  OTHER: "default",
};

export default function PetManagement() {
  const { user } = useAuthStore();
  const orgId = user?.orgId;

  const [form] = Form.useForm();
  const [pets, setPets] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [filters, setFilters] = useState({
    type: undefined,
    status: undefined,
  });
  const [modalVisible, setModalVisible] = useState(false);
  const [modalMode, setModalMode] = useState("create");
  const [currentPet, setCurrentPet] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [uploadingPetId, setUploadingPetId] = useState(null);

  const fetchPets = async (page, pageSize) => {
    setLoading(true);
    try {
      const params = {
        page,
        pageSize,
        type: filters.type || undefined,
        status: filters.status || undefined,
        orgId,
      };
      const res = await api.pets.fetchPets(params);
      if (res?.code === 200) {
        const { list = [], total = 0 } = res.data || {};
        setPets(list);
        setPagination((prev) => ({
          ...prev,
          current: page,
          pageSize,
          total,
        }));
      }
    } catch (error) {
      message.error(error?.message || "获取宠物列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPets(1, pagination.pageSize);
    setPagination((prev) => ({ ...prev, current: 1 }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters, orgId, pagination.pageSize]);

  const openCreateModal = () => {
    setModalMode("create");
    setCurrentPet(null);
    form.resetFields();
    form.setFieldsValue({
      status: "AVAILABLE",
      orgId,
    });
    setModalVisible(true);
  };

  const openEditModal = (pet) => {
    setModalMode("edit");
    setCurrentPet(pet);
    form.setFieldsValue({
      ...pet,
      orgId: pet.orgId || orgId,
    });
    setModalVisible(true);
  };

  const handleModalCancel = () => {
    setModalVisible(false);
    setCurrentPet(null);
    form.resetFields();
  };

  const handleFormSubmit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload = {
        ...values,
        orgId: values.orgId || orgId,
      };

      if (!payload.orgId) {
        message.error("缺少机构信息，无法提交");
        setSubmitting(false);
        return;
      }

      if (modalMode === "create") {
        await api.pets.createPet(payload);
        message.success("新增宠物成功");
      } else if (currentPet?.id) {
        await api.pets.updatePet(currentPet.id, payload);
        message.success("更新宠物信息成功");
      }

      setModalVisible(false);
      form.resetFields();
      setCurrentPet(null);
      fetchPets(pagination.current, pagination.pageSize);
    } catch (error) {
      if (error?.errorFields) {
        // 表单验证错误已由 antd 提示
      } else {
        message.error(error?.message || "提交失败，请稍后重试");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusUpdate = async (petId, status) => {
    try {
      await api.pets.updatePetStatus(petId, status);
      message.success("状态更新成功");
      fetchPets(pagination.current, pagination.pageSize);
    } catch (error) {
      message.error(error?.message || "状态更新失败");
    }
  };

  const handleUploadCover = async (petId, file) => {
    const allowedTypes = ["image/jpeg", "image/png", "image/gif"];
    if (!allowedTypes.includes(file.type)) {
      message.error("仅支持上传 JPG/PNG/GIF 图片");
      return Upload.LIST_IGNORE;
    }

    setUploadingPetId(petId);
    try {
      const res = await api.pets.uploadPetCover(petId, file);
      if (res?.code === 200) {
        message.success("封面上传成功");
        fetchPets(pagination.current, pagination.pageSize);
      } else {
        message.error(res?.message || "封面上传失败");
      }
    } catch (error) {
      message.error(error?.message || "封面上传失败");
    } finally {
      setUploadingPetId(null);
    }
    return Upload.LIST_IGNORE;
  };

  const columns = [
    {
      title: "封面",
      dataIndex: "coverUrl",
      key: "coverUrl",
      width: 120,
      render: (value) => (
        <div
          style={{
            width: 80,
            height: 80,
            borderRadius: 8,
            overflow: "hidden",
            background: "#f5f5f5",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            border: "1px solid #f0f0f0",
          }}
        >
          {value ? (
            <img
              src={value.startsWith("http") ? value : `/files/${value}`}
              alt="封面"
              style={{ width: "100%", height: "100%", objectFit: "cover" }}
            />
          ) : (
            <Text type="secondary" style={{ fontSize: 12 }}>
              暂无
            </Text>
          )}
        </div>
      ),
    },
    {
      title: "宠物名称",
      dataIndex: "name",
      key: "name",
      width: 180,
      render: (value, record) => (
        <Space direction="vertical" size={2}>
          <Space>
            <Text strong>{value || "-"}</Text>
            {record.type && (
              <Tag color={TYPE_COLOR_MAP[record.type] || "default"}>
                {PET_TYPE_OPTIONS.find((item) => item.value === record.type)
                  ?.label || record.type}
              </Tag>
            )}
          </Space>
          <Text type="secondary" style={{ fontSize: 12 }}>
            ID: {record.id}
          </Text>
        </Space>
      ),
    },
    {
      title: "基本信息",
      key: "basic",
      width: 220,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text>品种：{record.breed || "-"}</Text>
          <Text>
            性别：
            {PET_GENDER_OPTIONS.find((item) => item.value === record.gender)
              ?.label ||
              record.gender ||
              "-"}
          </Text>
          <Text>年龄：{record.age != null ? `${record.age} 岁` : "-"}</Text>
          <Text>
            体型：
            {PET_SIZE_OPTIONS.find((item) => item.value === record.size)
              ?.label ||
              record.size ||
              "-"}
          </Text>
          <Text>颜色：{record.color || "-"}</Text>
        </Space>
      ),
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 140,
      render: (value) => (
        <Tag color={STATUS_COLOR_MAP[value] || "default"}>
          {PET_STATUS_OPTIONS.find((item) => item.value === value)?.label ||
            value}
        </Tag>
      ),
    },
    {
      title: "描述",
      dataIndex: "description",
      key: "description",
      ellipsis: true,
    },
    {
      title: "更新时间",
      dataIndex: "updatedAt",
      key: "updatedAt",
      width: 180,
    },
    {
      title: "操作",
      key: "actions",
      fixed: "right",
      width: 260,
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            type="primary"
            icon={<EditOutlined />}
            onClick={() => openEditModal(record)}
          >
            编辑
          </Button>
          <Select
            size="small"
            value={record.status}
            style={{ width: 120 }}
            onChange={(status) => handleStatusUpdate(record.id, status)}
          >
            {PET_STATUS_OPTIONS.map((item) => (
              <Option key={item.value} value={item.value}>
                {item.label}
              </Option>
            ))}
          </Select>
          <Upload
            showUploadList={false}
            beforeUpload={(file) => handleUploadCover(record.id, file)}
            accept="image/jpeg,image/png,image/gif"
          >
            <Button
              size="small"
              icon={<UploadOutlined />}
              loading={uploadingPetId === record.id}
            >
              更新封面
            </Button>
          </Upload>
        </Space>
      ),
    },
  ];

  const handleTableChange = (nextPagination) => {
    fetchPets(nextPagination.current, nextPagination.pageSize);
  };

  return (
    <div>
      <Card bordered={false} style={{ marginBottom: 16 }}>
        <Space direction="vertical" style={{ width: "100%" }} size={12}>
          <Space style={{ width: "100%", justifyContent: "space-between" }}>
            <Title level={2} style={{ margin: 0 }}>
              发布/下架宠物
            </Title>
            <Space>
              <Button
                icon={<ReloadOutlined />}
                onClick={() =>
                  fetchPets(pagination.current, pagination.pageSize)
                }
              >
                刷新
              </Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={openCreateModal}
              >
                新增宠物
              </Button>
            </Space>
          </Space>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            支持根据宠物类型与状态进行筛选，新增宠物时请完整填写基本信息，上传封面图可提升曝光度。
          </Paragraph>
          <Space wrap>
            <Select
              allowClear
              placeholder="宠物类型"
              style={{ width: 160 }}
              value={filters.type}
              onChange={(value) =>
                setFilters((prev) => ({ ...prev, type: value }))
              }
            >
              {PET_TYPE_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
            <Select
              allowClear
              placeholder="宠物状态"
              style={{ width: 160 }}
              value={filters.status}
              onChange={(value) =>
                setFilters((prev) => ({ ...prev, status: value }))
              }
            >
              {PET_STATUS_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          </Space>
        </Space>
      </Card>

      <Card bordered={false}>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={pets}
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 只宠物`,
          }}
          onChange={handleTableChange}
          scroll={{ x: 1100 }}
        />
      </Card>

      <Modal
        title={modalMode === "create" ? "新增宠物" : "编辑宠物信息"}
        open={modalVisible}
        onCancel={handleModalCancel}
        onOk={handleFormSubmit}
        confirmLoading={submitting}
        width={720}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ status: "AVAILABLE" }}
        >
          <Form.Item
            name="orgId"
            label="机构ID"
            rules={[{ required: true, message: "请输入机构ID" }]}
          >
            <Input disabled={!!orgId} placeholder="请输入机构ID" />
          </Form.Item>

          <Divider orientation="left">基本信息</Divider>
          <Form.Item
            name="name"
            label="宠物名称"
            rules={[{ required: true, message: "请输入宠物名称" }]}
          >
            <Input placeholder="例如：Lucky" allowClear />
          </Form.Item>

          <Form.Item
            name="type"
            label="宠物类型"
            rules={[{ required: true, message: "请选择宠物类型" }]}
          >
            <Select placeholder="请选择类型" allowClear>
              {PET_TYPE_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="breed" label="品种">
            <Input placeholder="例如：金毛" allowClear />
          </Form.Item>

          <Form.Item name="gender" label="性别">
            <Select placeholder="请选择性别" allowClear>
              {PET_GENDER_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="age" label="年龄 (岁)">
            <InputNumber
              min={0}
              precision={0}
              style={{ width: "100%" }}
              placeholder="请输入整数年龄"
            />
          </Form.Item>

          <Form.Item name="color" label="颜色">
            <Input placeholder="例如：棕色" allowClear />
          </Form.Item>

          <Form.Item name="size" label="体型">
            <Select placeholder="请选择体型" allowClear>
              {PET_SIZE_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="status" label="状态">
            <Select placeholder="请选择状态">
              {PET_STATUS_OPTIONS.map((item) => (
                <Option key={item.value} value={item.value}>
                  {item.label}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Divider orientation="left">其他</Divider>
          <Form.Item name="coverUrl" label="封面地址">
            <Input
              placeholder="可填写已上传的图片链接，或在列表中上传封面"
              prefix={<PictureOutlined />}
              allowClear
            />
          </Form.Item>

          <Form.Item name="description" label="宠物介绍">
            <Input.TextArea
              rows={4}
              maxLength={500}
              placeholder="请输入宠物介绍，最多500字"
              showCount
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
