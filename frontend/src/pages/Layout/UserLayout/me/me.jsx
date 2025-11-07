import React, { useEffect, useState } from "react";
import {
  Modal,
  Form,
  Input,
  Button,
  Dropdown,
  App as AntdApp,
  Tabs,
  Segmented,
} from "antd";
import {
  UserOutlined,
  LogoutOutlined,
  MailOutlined,
  PhoneOutlined,
  LockOutlined,
  SettingOutlined,
  HeartOutlined,
  FileTextOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router";
import useAuthStore from "../../../../store/authStore";
import api from "../../../../api";
import styles from "./me.module.css";

export default function Me() {
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [activeKey, setActiveKey] = useState("login");
  const [loginType, setLoginType] = useState("client"); // "client" 或 "business"
  const [form] = Form.useForm();
  const [regForm] = Form.useForm();
  const { isLoggedIn, user, login, logout } = useAuthStore();
  const { message } = AntdApp.useApp();
  const navigate = useNavigate();
  const [sending, setSending] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [timer, setTimer] = useState(null);

  const handleShowLogin = () => {
    setActiveKey("login");
    setIsLoginModalOpen(true);
  };

  // 监听全局事件，唤起登录弹窗（供详情页等地方调用）
  useEffect(() => {
    const open = () => handleShowLogin();
    window.addEventListener("OPEN_LOGIN_MODAL", open);
    return () => window.removeEventListener("OPEN_LOGIN_MODAL", open);
  }, []);

  const handleLogin = async (values) => {
    try {
      const res = await api.auth.loginApi({
        username: values.username,
        password: values.password,
      });
      if (res?.code === 200 && res?.data) {
        const userData = res.data;
        const roles = userData.roles || [];

        // 保存用户信息
        login({ ...userData, token: userData.token });
        if (userData.token) {
          localStorage.setItem("token", userData.token);
        }

        message.success("登录成功");
        setIsLoginModalOpen(false);
        form.resetFields();

        // 根据登录类型和角色进行跳转
        if (loginType === "business") {
          // B端登录：检查是否有B端角色
          const businessRoles = [
            "ADMIN",
            "AUDITOR",
            "CS",
            "ORG_ADMIN",
            "ORG_STAFF",
          ];
          const hasBusinessRole = roles.some((role) =>
            businessRoles.includes(role)
          );

          if (hasBusinessRole) {
            navigate("/admin/dashboard");
          } else {
            message.warning("该账号没有B端权限，将以C端身份登录");
            // 继续留在C端页面
          }
        } else {
          // C端登录：如果只有B端角色，提示用户
          const businessRoles = [
            "ADMIN",
            "AUDITOR",
            "CS",
            "ORG_ADMIN",
            "ORG_STAFF",
          ];
          const onlyBusinessRoles =
            roles.length > 0 &&
            roles.every((role) => businessRoles.includes(role)) &&
            !roles.includes("USER");

          if (onlyBusinessRoles) {
            message.warning("该账号为B端账号，请选择B端登录");
          }
          // C端用户继续留在当前页面
        }
      } else {
        message.error(res?.message || "登录失败");
      }
    } catch (error) {
      message.error(error?.message || "登录失败，请稍后重试");
    }
  };

  const handleSendCode = async () => {
    try {
      const email = regForm.getFieldValue("email");
      if (!email) {
        message.warning("请先填写邮箱");
        return;
      }
      await regForm.validateFields(["email"]);
      setSending(true);
      const res = await api.auth.sendRegisterCodeApi(email);
      if (res?.code === 200) {
        message.success("验证码已发送，请查收邮箱");
        // 开始 60s 倒计时
        setCountdown(60);
        const t = setInterval(() => {
          setCountdown((c) => {
            if (c <= 1) {
              clearInterval(t);
              setTimer(null);
              return 0;
            }
            return c - 1;
          });
        }, 1000);
        setTimer(t);
      } else {
        message.error(res?.message || "发送失败");
      }
    } catch (e) {
      message.error(e?.message || "发送失败，请稍后重试");
    } finally {
      setSending(false);
    }
  };

  const handleRegister = async (values) => {
    try {
      const res = await api.auth.registerApi({
        username: values.username,
        password: values.password,
        email: values.email,
        phone: values.phone,
        code: values.code,
      });
      if (res?.code === 200) {
        message.success("注册成功，请登录");
        // 切回登录页并预填用户名
        setActiveKey("login");
        form.setFieldsValue({ username: values.username });
        regForm.resetFields();
      } else {
        message.error(res?.message || "注册失败");
      }
    } catch (error) {
      message.error(error?.message || "注册失败，请稍后重试");
    }
  };

  const handleLogout = () => {
    logout();
    localStorage.removeItem("token");
    message.success("已退出登录");
  };

  const getUserDisplay = () => {
    if (user?.username) {
      return user.username.charAt(0).toUpperCase();
    }
    return "?";
  };

  const getAvatarUrl = () => {
    if (user?.avatarUrl) {
      // 如果是完整URL，直接使用；如果是相对路径，需要拼接
      if (user.avatarUrl.startsWith("http") || user.avatarUrl.startsWith("/")) {
        return user.avatarUrl;
      }
      return `/files/${user.avatarUrl}`;
    }
    return null;
  };

  const menuItems = [
    {
      key: "wishlist",
      label: "查看领养愿望单",
      icon: <HeartOutlined />,
      onClick: () => {
        navigate("/wishlist");
      },
    },
    {
      key: "community-settings",
      label: "设置社区资料",
      icon: <SettingOutlined />,
      onClick: () => {
        navigate("/settings/community");
      },
    },
    {
      key: "adoption-settings",
      label: "设置领养资料",
      icon: <FileTextOutlined />,
      onClick: () => {
        // TODO: 实现领养资料设置页面
        message.info("领养资料设置功能开发中");
      },
    },
    {
      type: "divider",
    },
    {
      key: "logout",
      label: "退出登录",
      icon: <LogoutOutlined />,
      onClick: handleLogout,
    },
  ];

  const loggedInCircle = (
    <div
      className={`${styles.meCircle} ${styles.loggedIn}`}
      title={user?.username || "用户"}
    >
      {getAvatarUrl() ? (
        <img
          src={getAvatarUrl()}
          alt={user?.username || "用户"}
          style={{
            width: "100%",
            height: "100%",
            borderRadius: "50%",
            objectFit: "cover",
          }}
        />
      ) : (
        <div className={styles.userAvatar}>{getUserDisplay()}</div>
      )}
    </div>
  );

  return (
    <>
      {isLoggedIn ? (
        <Dropdown menu={{ items: menuItems }} placement="bottomRight">
          {loggedInCircle}
        </Dropdown>
      ) : (
        <div
          className={styles.meCircle}
          onClick={handleShowLogin}
          title="点击登录/注册"
        >
          <span className={styles.loginText}>登录</span>
        </div>
      )}

      <Modal
        title={activeKey === "login" ? "登录" : "注册"}
        open={isLoginModalOpen}
        onCancel={() => {
          setIsLoginModalOpen(false);
          form.resetFields();
          regForm.resetFields();
          setLoginType("client");
          if (timer) {
            clearInterval(timer);
            setTimer(null);
          }
          setCountdown(0);
          setSending(false);
        }}
        footer={null}
        width={420}
      >
        <Tabs
          activeKey={activeKey}
          onChange={(k) => setActiveKey(k)}
          items={[
            {
              key: "login",
              label: "登录",
              children: (
                <Form
                  form={form}
                  name="login"
                  onFinish={handleLogin}
                  layout="vertical"
                  autoComplete="off"
                >
                  <Form.Item label="登录身份">
                    <Segmented
                      options={[
                        { label: "C端用户", value: "client" },
                        { label: "B端管理", value: "business" },
                      ]}
                      value={loginType}
                      onChange={setLoginType}
                      block
                    />
                  </Form.Item>

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
                    label="密码"
                    name="password"
                    rules={[{ required: true, message: "请输入密码" }]}
                  >
                    <Input.Password
                      prefix={<LockOutlined />}
                      placeholder="请输入密码"
                    />
                  </Form.Item>

                  <Form.Item>
                    <Button type="primary" htmlType="submit" block>
                      登录
                    </Button>
                  </Form.Item>
                </Form>
              ),
            },
            {
              key: "register",
              label: "注册",
              children: (
                <Form
                  form={regForm}
                  name="register"
                  onFinish={handleRegister}
                  layout="vertical"
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
                    label="密码"
                    name="password"
                    rules={[{ required: true, message: "请输入密码" }]}
                  >
                    <Input.Password
                      prefix={<LockOutlined />}
                      placeholder="请输入密码"
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

                  <Form.Item
                    label="邮箱"
                    name="email"
                    rules={[
                      { required: true, message: "请输入邮箱" },
                      { type: "email", message: "邮箱格式不正确" },
                    ]}
                  >
                    <Input prefix={<MailOutlined />} placeholder="请输入邮箱" />
                  </Form.Item>

                  <Form.Item label="验证码" required>
                    <Input.Group compact>
                      <Form.Item
                        name="code"
                        noStyle
                        rules={[{ required: true, message: "请输入验证码" }]}
                      >
                        <Input
                          style={{ width: "60%" }}
                          placeholder="请输入验证码"
                        />
                      </Form.Item>
                      <Button
                        style={{ width: "40%" }}
                        onClick={handleSendCode}
                        disabled={sending || countdown > 0}
                      >
                        {countdown > 0 ? `${countdown}s` : "获取验证码"}
                      </Button>
                    </Input.Group>
                  </Form.Item>

                  <Form.Item>
                    <Button type="primary" htmlType="submit" block>
                      注册
                    </Button>
                  </Form.Item>
                </Form>
              ),
            },
          ]}
        />
      </Modal>
    </>
  );
}
