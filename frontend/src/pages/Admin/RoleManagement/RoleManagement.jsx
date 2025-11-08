import React, { useState, useEffect } from "react";
import {
  Table,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  Select,
  Input,
  App as AntdApp,
  Typography,
  Card,
} from "antd";
import {
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import api from "../../../api";

const { Title } = Typography;
const { Option } = Select;

// 角色选项（超级管理员可分配的角色，排除ADMIN和USER）
// ADMIN需要通过其他方式分配，USER是默认角色
const ASSIGNABLE_ROLES = [
  { value: "AUDITOR", label: "审核员" },
  { value: "CS", label: "客服人员" },
  { value: "ORG_ADMIN", label: "机构管理员" },
  { value: "ORG_STAFF", label: "宠物信息维护员" },
];

// 角色显示映射
const ROLE_LABELS = {
  USER: "普通用户",
  ORG_ADMIN: "机构管理员",
  ORG_STAFF: "宠物信息维护员",
  AUDITOR: "审核员",
  CS: "客服人员",
  ADMIN: "超级管理员",
};

// 角色颜色映射
const ROLE_COLORS = {
  USER: "default",
  ORG_ADMIN: "blue",
  ORG_STAFF: "cyan",
  AUDITOR: "orange",
  CS: "green",
  ADMIN: "red",
};

export default function RoleManagement() {
  const { message } = AntdApp.useApp();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });
  const [assignModalVisible, setAssignModalVisible] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [assignForm] = Form.useForm();
  const [searchKeyword, setSearchKeyword] = useState("");

  // 获取用户列表
  const fetchUsers = async (page = 1, pageSize = 10) => {
    setLoading(true);
    try {
      const res = await api.auth.getUserListApi({ page, pageSize });
      if (res?.code === 200 && res?.data) {
        let userList = res.data.list || [];

        // 如果有搜索关键词，进行过滤
        if (searchKeyword) {
          const keyword = searchKeyword.toLowerCase();
          userList = userList.filter(
            (user) =>
              user.username?.toLowerCase().includes(keyword) ||
              user.email?.toLowerCase().includes(keyword) ||
              user.phone?.includes(keyword)
          );
        }

        setUsers(userList);
        setPagination({
          current: page,
          pageSize: pageSize,
          total: res.data.total || 0,
        });
      }
    } catch (error) {
      message.error(error?.message || "获取用户列表失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers(pagination.current, pagination.pageSize);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 分配角色
  const handleAssignRole = async (values) => {
    try {
      const res = await api.auth.assignRoleApi({
        userId: selectedUser.id,
        role: values.role,
      });
      if (res?.code === 200) {
        message.success("角色分配成功");
        setAssignModalVisible(false);
        assignForm.resetFields();
        fetchUsers(pagination.current, pagination.pageSize);
      } else {
        message.error(res?.message || "角色分配失败");
      }
    } catch (error) {
      message.error(error?.message || "角色分配失败，请稍后重试");
    }
  };

  // 移除角色
  const handleRemoveRole = async (userId, role) => {
    try {
      const res = await api.auth.removeRoleApi({ userId, role });
      if (res?.code === 200) {
        message.success("角色移除成功");
        fetchUsers(pagination.current, pagination.pageSize);
      } else {
        message.error(res?.message || "角色移除失败");
      }
    } catch (error) {
      message.error(error?.message || "角色移除失败，请稍后重试");
    }
  };

  // 打开分配角色弹窗
  const openAssignModal = (user) => {
    setSelectedUser(user);
    setAssignModalVisible(true);
    assignForm.resetFields();
  };

  // 处理搜索
  const handleSearch = () => {
    fetchUsers(1, pagination.pageSize);
  };

  // 处理分页变化
  const handleTableChange = (newPagination) => {
    fetchUsers(newPagination.current, newPagination.pageSize);
  };

  const columns = [
    {
      title: "用户ID",
      dataIndex: "id",
      key: "id",
      width: 80,
    },
    {
      title: "用户名",
      dataIndex: "username",
      key: "username",
      width: 120,
    },
    {
      title: "邮箱",
      dataIndex: "email",
      key: "email",
      width: 180,
    },
    {
      title: "手机号",
      dataIndex: "phone",
      key: "phone",
      width: 120,
    },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (status) => (
        <Tag color={status === "ACTIVE" ? "success" : "error"}>
          {status === "ACTIVE" ? "正常" : "禁用"}
        </Tag>
      ),
    },
    {
      title: "角色",
      dataIndex: "roles",
      key: "roles",
      render: (roles) => (
        <Space wrap>
          {roles?.map((role) => (
            <Tag key={role} color={ROLE_COLORS[role] || "default"}>
              {ROLE_LABELS[role] || role}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: "注册时间",
      dataIndex: "createdAt",
      key: "createdAt",
      width: 180,
    },
    {
      title: "操作",
      key: "action",
      width: 200,
      fixed: "right",
      render: (_, record) => {
        const currentRoles = record.roles || [];
        // 过滤出可移除的角色（排除ADMIN，且不能是最后一个角色）
        const removableRoles = currentRoles.filter(
          (role) => role !== "ADMIN" && currentRoles.length > 1
        );

        return (
          <Space>
            <Button
              type="primary"
              size="small"
              icon={<PlusOutlined />}
              onClick={() => openAssignModal(record)}
            >
              分配角色
            </Button>
            {removableRoles.length > 0 && (
              <Select
                size="small"
                placeholder="移除角色"
                style={{ width: 120 }}
                onSelect={(role) => {
                  Modal.confirm({
                    title: "确定要移除该角色吗？",
                    content: `确定要移除角色"${ROLE_LABELS[role] || role}"吗？`,
                    onOk: () => handleRemoveRole(record.id, role),
                    okText: "确定",
                    cancelText: "取消",
                  });
                }}
              >
                {removableRoles.map((role) => (
                  <Option key={role} value={role}>
                    {ROLE_LABELS[role] || role}
                  </Option>
                ))}
              </Select>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <div>
      <Card>
        <Space
          style={{
            marginBottom: 16,
            width: "100%",
            justifyContent: "space-between",
          }}
        >
          <Title level={2} style={{ margin: 0 }}>
            平台角色管理
          </Title>
          <Space>
            <Input
              placeholder="搜索用户名/邮箱/手机号"
              prefix={<SearchOutlined />}
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onPressEnter={handleSearch}
              style={{ width: 250 }}
              allowClear
            />
            <Button icon={<SearchOutlined />} onClick={handleSearch}>
              搜索
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                setSearchKeyword("");
                fetchUsers(pagination.current, pagination.pageSize);
              }}
            >
              刷新
            </Button>
          </Space>
        </Space>

        <Table
          columns={columns}
          dataSource={users}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
          onChange={handleTableChange}
          scroll={{ x: 1200 }}
        />
      </Card>

      <Modal
        title="分配角色"
        open={assignModalVisible}
        onCancel={() => {
          setAssignModalVisible(false);
          assignForm.resetFields();
        }}
        footer={null}
        width={500}
      >
        <Form
          form={assignForm}
          layout="vertical"
          onFinish={handleAssignRole}
          initialValues={{ role: undefined }}
        >
          <Form.Item label="用户信息">
            <Input
              value={
                selectedUser
                  ? `${selectedUser.username} (${selectedUser.email})`
                  : ""
              }
              disabled
            />
          </Form.Item>
          <Form.Item label="当前角色">
            <Space wrap>
              {selectedUser?.roles?.map((role) => (
                <Tag key={role} color={ROLE_COLORS[role] || "default"}>
                  {ROLE_LABELS[role] || role}
                </Tag>
              ))}
            </Space>
          </Form.Item>
          <Form.Item
            name="role"
            label="选择要分配的角色"
            rules={[{ required: true, message: "请选择要分配的角色" }]}
          >
            <Select placeholder="请选择角色" allowClear>
              {ASSIGNABLE_ROLES.map((role) => {
                const hasRole = selectedUser?.roles?.includes(role.value);
                return (
                  <Option
                    key={role.value}
                    value={role.value}
                    disabled={hasRole}
                  >
                    {role.label}
                    {hasRole && " (已拥有)"}
                  </Option>
                );
              })}
            </Select>
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                确定分配
              </Button>
              <Button
                onClick={() => {
                  setAssignModalVisible(false);
                  assignForm.resetFields();
                }}
              >
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
