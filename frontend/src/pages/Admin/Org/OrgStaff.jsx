import React, { useEffect, useState } from "react";
import {
  Card,
  Table,
  Space,
  Button,
  Modal,
  Form,
  InputNumber,
  App as AntdApp,
  Typography,
  Empty,
  Select,
} from "antd";
import {
  PlusOutlined,
  DeleteOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import api from "../../../api";
import useAuthStore from "../../../store/authStore";

const { Title, Text } = Typography;

export default function OrgStaff() {
  const { message } = AntdApp.useApp();
  const { user } = useAuthStore();
  const userId = user?.id || user?.userId;

  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();
  const [orgOptions, setOrgOptions] = useState([]);
  const [selectedOrgId, setSelectedOrgId] = useState(null);
  const [orgLoading, setOrgLoading] = useState(false);

  const fetchMemberships = async () => {
    if (!userId) return;
    setOrgLoading(true);
    try {
      const res = await api.org.getUserMemberships(userId);
      if (res?.code === 200) {
        const list = Array.isArray(res.data)
          ? res.data
          : res.data?.list || res.data?.memberships || [];
        const mapped = list
          .map((item) => {
            const org = item.organizationId ? item : item.org || item;
            const id = org.orgId || org.organizationId || org.id;
            if (!id) return null;
            return {
              label: org.name || `机构 ${id}`,
              value: id,
            };
          })
          .filter(Boolean);
        setOrgOptions(mapped);
        if (mapped.length > 0) {
          setSelectedOrgId((prev) => prev ?? mapped[0].value);
        } else {
          setSelectedOrgId(null);
        }
      }
    } catch (e) {
      message.error(e?.message || "获取机构列表失败");
    } finally {
      setOrgLoading(false);
    }
  };

  const fetchMembers = async () => {
    if (!selectedOrgId) return;
    setLoading(true);
    try {
      const res = await api.org.getOrgMembers(selectedOrgId);
      if (res?.code === 200) {
        const list = Array.isArray(res.data) ? res.data : res.data?.list || [];
        setMembers(list);
      }
    } catch (e) {
      message.error(e?.message || "获取成员列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMemberships();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  useEffect(() => {
    fetchMembers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedOrgId]);

  const handleAddMember = async () => {
    if (!selectedOrgId) {
      message.warning("请选择机构后再添加成员");
      return;
    }
    try {
      const values = await form.validateFields();
      await api.org.addOrgMember(selectedOrgId, { userId: values.userId });
      message.success("已添加成员");
      setModalVisible(false);
      form.resetFields();
      fetchMembers();
    } catch (e) {
      if (e?.errorFields) return;
      message.error(e?.message || "添加失败");
    }
  };

  const handleRemove = async (member) => {
    Modal.confirm({
      title: "确认移除成员?",
      content: `确认从机构中移除用户 ${member.userId || member.id} 吗？`,
      onOk: async () => {
        try {
          const targetUserId = member.userId || member.id;
          await api.org.removeOrgMember(selectedOrgId, targetUserId);
          message.success("已移除成员");
          fetchMembers();
        } catch (e) {
          message.error(e?.message || "移除失败");
        }
      },
    });
  };

  const columns = [
    {
      title: "成员ID",
      dataIndex: "userId",
      key: "userId",
      render: (value, record) => value ?? record.id,
    },
    {
      title: "用户名",
      dataIndex: "username",
      key: "username",
      render: (value) => value || "-",
    },
    {
      title: "邮箱",
      dataIndex: "email",
      key: "email",
      render: (value) => value || "-",
    },
    {
      title: "加入时间",
      dataIndex: "membershipCreatedAt",
      key: "membershipCreatedAt",
      render: (value, record) => value || record?.createdAt || "-",
    },
    {
      title: "操作",
      key: "action",
      width: 160,
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            icon={<DeleteOutlined />}
            danger
            onClick={() => handleRemove(record)}
          >
            移除
          </Button>
        </Space>
      ),
    },
  ];

  if (orgOptions.length === 0 && !orgLoading) {
    return (
      <Card>
        <Text>当前账号未关联机构，请先提交入驻申请或联系管理员。</Text>
      </Card>
    );
  }

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Space style={{ width: "100%", justifyContent: "space-between" }}>
          <Title level={3} style={{ margin: 0 }}>
            机构成员管理
          </Title>
          <Space>
            <Select
              placeholder="选择机构"
              loading={orgLoading}
              value={selectedOrgId}
              style={{ width: 220 }}
              onChange={(value) => setSelectedOrgId(value || null)}
              options={orgOptions}
              allowClear={orgOptions.length > 1}
            />
            <Button
              icon={<ReloadOutlined />}
              onClick={fetchMembers}
              disabled={!selectedOrgId}
            >
              刷新
            </Button>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => {
                form.setFieldsValue({ orgId: selectedOrgId });
                setModalVisible(true);
              }}
              disabled={!selectedOrgId}
            >
              添加成员
            </Button>
          </Space>
        </Space>
        <Text type="secondary">
          {selectedOrgId ? `机构ID：${selectedOrgId}` : "请选择机构后进行管理"}
        </Text>
      </Card>

      <Card>
        <Table
          rowKey={(record) =>
            record.membershipId || `${record.userId || record.id}`
          }
          columns={columns}
          dataSource={members}
          loading={loading}
          locale={{ emptyText: <Empty description="暂无成员" /> }}
        />
      </Card>

      <Modal
        title="添加机构成员"
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false);
          form.resetFields();
        }}
        onOk={handleAddMember}
        okText="确认添加"
      >
        <Form layout="vertical" form={form}>
          <Form.Item
            label="用户ID"
            name="userId"
            rules={[{ required: true, message: "请输入用户ID" }]}
          >
            <InputNumber
              style={{ width: "100%" }}
              min={1}
              precision={0}
              placeholder="输入需要加入机构的用户ID"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
