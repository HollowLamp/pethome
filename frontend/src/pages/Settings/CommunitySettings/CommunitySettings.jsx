import React, { useState, useEffect } from "react";
import {
  Card,
  Form,
  Input,
  Button,
  Upload,
  Avatar,
  message,
  Space,
  Typography,
  Divider,
} from "antd";
import {
  UserOutlined,
  MailOutlined,
  PhoneOutlined,
  LockOutlined,
  UploadOutlined,
  CameraOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router";
import useAuthStore from "../../../store/authStore";
import api from "../../../api";
import "./CommunitySettings.module.css";

const { Title } = Typography;

export default function CommunitySettings() {
  const [form] = Form.useForm();
  const [passwordForm] = Form.useForm();
  const navigate = useNavigate();
  const { user, updateUser } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [avatarLoading, setAvatarLoading] = useState(false);
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl || null);

  useEffect(() => {
    if (!user) {
      navigate("/");
      return;
    }
    // 初始化表单数据
    form.setFieldsValue({
      username: user.username,
      email: user.email,
      phone: user.phone,
    });
    setAvatarUrl(user.avatarUrl);
  }, [user, form, navigate]);

  // 上传头像
  const handleAvatarUpload = async (file) => {
    setAvatarLoading(true);
    try {
      const res = await api.auth.uploadAvatarApi(file);
      if (res?.code === 200 && res?.data) {
        const userData = res.data;
        updateUser(userData);
        setAvatarUrl(userData.avatarUrl);
        message.success("头像上传成功");
      } else {
        message.error(res?.message || "头像上传失败");
      }
    } catch (error) {
      message.error(error?.message || "头像上传失败，请稍后重试");
    } finally {
      setAvatarLoading(false);
    }
    return false; // 阻止自动上传
  };

  // 更新用户信息
  const handleUpdateUser = async (values) => {
    setLoading(true);
    try {
      const res = await api.auth.updateUserApi({
        username: values.username,
        email: values.email,
        phone: values.phone,
      });
      if (res?.code === 200 && res?.data) {
        updateUser(res.data);
        message.success("资料更新成功");
      } else {
        message.error(res?.message || "资料更新失败");
      }
    } catch (error) {
      message.error(error?.message || "资料更新失败，请稍后重试");
    } finally {
      setLoading(false);
    }
  };

  // 更新密码
  const handleUpdatePassword = async (values) => {
    setPasswordLoading(true);
    try {
      const res = await api.auth.updatePasswordApi({
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      if (res?.code === 200) {
        message.success("密码修改成功");
        passwordForm.resetFields();
      } else {
        message.error(res?.message || "密码修改失败");
      }
    } catch (error) {
      message.error(error?.message || "密码修改失败，请稍后重试");
    } finally {
      setPasswordLoading(false);
    }
  };

  // 获取头像显示
  const getAvatarDisplay = () => {
    if (avatarUrl) {
      // 如果是完整URL，直接使用；如果是相对路径，需要拼接
      if (avatarUrl.startsWith("http") || avatarUrl.startsWith("/")) {
        return avatarUrl;
      }
      return `/files/${avatarUrl}`;
    }
    return null;
  };

  return (
    <div style={{ maxWidth: 800, margin: "0 auto", padding: "24px" }}>
      <Title level={2}>设置社区资料</Title>

      <Card title="头像设置" style={{ marginBottom: 24 }}>
        <Space direction="vertical" align="center" style={{ width: "100%" }}>
          <Avatar
            size={120}
            src={getAvatarDisplay()}
            icon={!getAvatarDisplay() && <UserOutlined />}
            style={{ marginBottom: 16 }}
          />
          <Upload
            beforeUpload={handleAvatarUpload}
            showUploadList={false}
            accept="image/*"
            maxCount={1}
          >
            <Button
              icon={<CameraOutlined />}
              loading={avatarLoading}
              disabled={avatarLoading}
            >
              上传头像
            </Button>
          </Upload>
          <div style={{ color: "#999", fontSize: 12 }}>
            支持 JPG、PNG、GIF 格式，文件大小不超过 10MB
          </div>
        </Space>
      </Card>

      <Card title="基本信息" style={{ marginBottom: 24 }}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleUpdateUser}
          autoComplete="off"
        >
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: "请输入用户名" }]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="请输入用户名"
            />
          </Form.Item>

          <Form.Item
            label="邮箱"
            name="email"
            rules={[
              { required: true, message: "请输入邮箱" },
              { type: "email", message: "邮箱格式不正确" },
            ]}
          >
            <Input
              prefix={<MailOutlined />}
              placeholder="请输入邮箱"
            />
          </Form.Item>

          <Form.Item
            label="手机号"
            name="phone"
            rules={[
              { required: true, message: "请输入手机号" },
              { pattern: /^\d{11}$/, message: "请输入11位手机号" },
            ]}
          >
            <Input
              prefix={<PhoneOutlined />}
              placeholder="请输入手机号"
            />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              保存修改
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card title="修改密码">
        <Form
          form={passwordForm}
          layout="vertical"
          onFinish={handleUpdatePassword}
          autoComplete="off"
        >
          <Form.Item
            label="原密码"
            name="oldPassword"
            rules={[{ required: true, message: "请输入原密码" }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请输入原密码"
            />
          </Form.Item>

          <Form.Item
            label="新密码"
            name="newPassword"
            rules={[
              { required: true, message: "请输入新密码" },
              { min: 6, message: "密码长度至少6位" },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请输入新密码"
            />
          </Form.Item>

          <Form.Item
            label="确认新密码"
            name="confirmPassword"
            dependencies={["newPassword"]}
            rules={[
              { required: true, message: "请确认新密码" },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue("newPassword") === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error("两次输入的密码不一致"));
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="请再次输入新密码"
            />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={passwordLoading} block>
              修改密码
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}

