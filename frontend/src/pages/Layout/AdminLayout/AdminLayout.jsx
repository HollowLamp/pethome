import React, { useState, useEffect } from "react";
import { Outlet, useNavigate, useLocation } from "react-router";
import {
  Layout,
  Menu,
  Avatar,
  Dropdown,
  Space,
  Typography,
  theme,
  App as AntdApp,
  Badge,
} from "antd";
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  LogoutOutlined,
  UserOutlined,
  SettingOutlined,
  HomeOutlined,
  TeamOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  HeartOutlined,
  MessageOutlined,
  ClockCircleOutlined,
  MedicineBoxOutlined,
  FileSearchOutlined,
  StarOutlined,
  AuditOutlined,
  CustomerServiceOutlined,
  ShopOutlined,
  UserSwitchOutlined,
  CalendarOutlined,
  CheckSquareOutlined,
} from "@ant-design/icons";
import useAuthStore from "../../../store/authStore";
import api from "../../../api";
import "./AdminLayout.module.css";

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

// 菜单配置
const getMenuItems = (roles) => {
  const items = [];
  const roleSet = new Set(roles || []);

  // 所有角色都显示首页
  items.push({
    key: "/admin/dashboard",
    icon: <HomeOutlined />,
    label: "首页",
  });

  // 所有角色都显示消息
  items.push({
    key: "/admin/messages",
    icon: <MessageOutlined />,
    label: "系统通知",
  });

  // 超级管理员菜单
  if (roleSet.has("ADMIN")) {
    items.push(
      {
        key: "/admin/role-management",
        icon: <TeamOutlined />,
        label: "分配平台角色",
      },
      {
        key: "/admin/featured-content",
        icon: <StarOutlined />,
        label: "推荐优秀案例",
      },
      {
        key: "/admin/appeals",
        icon: <AuditOutlined />,
        label: "裁定违规申诉",
      }
    );
  }

  // 审核员菜单
  if (roleSet.has("AUDITOR")) {
    items.push(
      {
        key: "/admin/org-approval",
        icon: <FileTextOutlined />,
        label: "审核机构入驻",
      },
      {
        key: "/admin/adoption-review",
        icon: <CheckCircleOutlined />,
        label: "审核领养申请（复审）",
      }
    );
  }

  // 客服人员菜单
  if (roleSet.has("CS")) {
    items.push(
      {
        key: "/admin/violation-review",
        icon: <WarningOutlined />,
        label: "复核违规内容",
      },
      {
        key: "/admin/user-reminders",
        icon: <ClockCircleOutlined />,
        label: "提醒更新状态",
      },
      {
        key: "/admin/user-appeals",
        icon: <MessageOutlined />,
        label: "处理用户申诉",
      }
    );
  }

  // 机构管理员菜单
  if (roleSet.has("ORG_ADMIN")) {
    items.push(
      {
        key: "/admin/org-application",
        icon: <ShopOutlined />,
        label: "申请机构入驻",
      },
      {
        key: "/admin/org-staff",
        icon: <UserSwitchOutlined />,
        label: "管理机构账号",
      },
      {
        key: "/admin/pet-management",
        icon: <HeartOutlined />,
        label: "发布/下架宠物",
      },
      {
        key: "/admin/adoption-approval",
        icon: <CheckCircleOutlined />,
        label: "审核用户申请（初审）",
      },
      {
        key: "/admin/interview-schedule",
        icon: <CalendarOutlined />,
        label: "安排面谈时间",
      },
      {
        key: "/admin/adoption-confirm",
        icon: <CheckSquareOutlined />,
        label: "确认领养交接",
      }
    );
  }

  // 宠物信息维护员菜单
  if (roleSet.has("ORG_STAFF")) {
    items.push(
      {
        key: "/admin/pet-health",
        icon: <MedicineBoxOutlined />,
        label: "更新宠物健康信息",
      },
      {
        key: "/admin/feedback-archive",
        icon: <FileSearchOutlined />,
        label: "查看并归档用户反馈",
      }
    );
  }

  return items;
};

export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuthStore();
  const { message } = AntdApp.useApp();

  const roles = user?.roles || [];
  const baseMenuItems = getMenuItems(roles);

  // 动态更新消息菜单项的标签，添加未读提示
  const menuItems = baseMenuItems.map((item) => {
    if (item.key === "/admin/messages") {
      return {
        ...item,
        label: (
          <span>
            {item.icon}
            <span style={{ marginLeft: 8 }}>系统通知</span>
            {unreadCount > 0 && (
              <Badge count={unreadCount} style={{ marginLeft: 8 }} />
            )}
          </span>
        ),
      };
    }
    return item;
  });

  useEffect(() => {
    // 加载未读消息数量
    loadUnreadCount();
    // 设置定时刷新
    const timer = setInterval(loadUnreadCount, 30000); // 每30秒刷新一次
    return () => clearInterval(timer);
  }, []);

  const loadUnreadCount = async () => {
    try {
      const res = await api.notification.getOrgSystemMessages().catch(() => ({ data: [] }));
      const unread = (res.data || []).filter((msg) => !msg.isRead).length;
      setUnreadCount(unread);
    } catch (error) {
      console.error("加载未读消息数量失败:", error);
    }
  };

  // 检查是否有B端权限
  const businessRoles = ["ADMIN", "AUDITOR", "CS", "ORG_ADMIN", "ORG_STAFF"];
  const hasBusinessRole = roles.some((role) => businessRoles.includes(role));

  useEffect(() => {
    // 如果没有B端权限，重定向到首页
    if (!hasBusinessRole) {
      message.warning("您没有B端管理权限");
      navigate("/");
    }
  }, [hasBusinessRole, navigate, message]);

  const handleMenuClick = ({ key }) => {
    navigate(key);
  };

  const handleLogout = () => {
    logout();
    localStorage.removeItem("token");
    message.success("已退出登录");
    navigate("/");
  };

  const getUserDisplay = () => {
    if (user?.username) {
      return user.username.charAt(0).toUpperCase();
    }
    return "?";
  };

  const userMenuItems = [
    {
      key: "logout",
      label: "退出登录",
      icon: <LogoutOutlined />,
      onClick: handleLogout,
    },
  ];

  // 如果没有B端权限，不渲染布局
  if (!hasBusinessRole) {
    return null;
  }

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        style={{
          background: token.colorBgContainer,
          boxShadow: "2px 0 8px 0 rgba(29,35,41,.05)",
        }}
      >
        <div
          style={{
            height: 64,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            borderBottom: `1px solid ${token.colorBorder}`,
          }}
        >
          {collapsed ? (
            <Text strong style={{ fontSize: 20, color: token.colorPrimary }}>
              P
            </Text>
          ) : (
            <Text strong style={{ fontSize: 18 }}>
              宠物领养管理平台
            </Text>
          )}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
          style={{ borderRight: 0, marginTop: 8 }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            position: "sticky",
            top: 0,
            zIndex: 100,
            padding: "0 24px",
            background: token.colorBgContainer,
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            boxShadow: "0 2px 8px 0 rgba(29,35,41,.05)",
          }}
        >
          <div
            style={{
              fontSize: 18,
              cursor: "pointer",
            }}
            onClick={() => setCollapsed(!collapsed)}
          >
            {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          </div>
          <Space size="middle">
            {unreadCount > 0 ? (
              <Badge dot offset={[-2, 2]}>
                <MessageOutlined
                  style={{
                    fontSize: 20,
                    cursor: "pointer",
                    color: location.pathname === "/admin/messages" ? token.colorPrimary : undefined,
                  }}
                  onClick={() => navigate("/admin/messages")}
                />
              </Badge>
            ) : (
              <MessageOutlined
                style={{
                  fontSize: 20,
                  cursor: "pointer",
                  color: location.pathname === "/admin/messages" ? token.colorPrimary : undefined,
                }}
                onClick={() => navigate("/admin/messages")}
              />
            )}
            <Text>{user?.username || "管理员"}</Text>
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <Avatar
                style={{
                  backgroundColor: token.colorPrimary,
                  cursor: "pointer",
                }}
                icon={<UserOutlined />}
              >
                {getUserDisplay()}
              </Avatar>
            </Dropdown>
          </Space>
        </Header>
        <Content
          style={{
            margin: "24px",
            padding: 24,
            minHeight: 280,
            background: token.colorBgContainer,
            borderRadius: token.borderRadius,
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
