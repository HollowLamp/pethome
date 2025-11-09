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
const getMenuItems = (roles, unreadCount) => {
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
    label: (
      <span>
        系统通知
        {unreadCount > 0 && (
          <Badge count={unreadCount} style={{ marginLeft: 8 }} />
        )}
      </span>
    ),
  });

  // 超级管理员菜单
  if (roleSet.has("ADMIN")) {
    items.push({
      key: "admin-group",
      icon: <TeamOutlined />,
      label: "超级管理员",
      children: [
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
      ],
    });
  }

  // 审核员菜单
  if (roleSet.has("AUDITOR")) {
    items.push({
      key: "auditor-group",
      icon: <FileTextOutlined />,
      label: "审核员",
      children: [
        {
          key: "/admin/org-approval",
          icon: <FileTextOutlined />,
          label: "审核机构入驻",
        },
        {
          key: "/admin/adoption-review",
          icon: <CheckCircleOutlined />,
          label: "审核领养申请（复审）",
        },
      ],
    });
  }

  // 客服人员菜单
  if (roleSet.has("CS")) {
    items.push({
      key: "cs-group",
      icon: <CustomerServiceOutlined />,
      label: "客服人员",
      children: [
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
        },
      ],
    });
  }

  // 机构管理员菜单
  if (roleSet.has("ORG_ADMIN")) {
    items.push({
      key: "org-admin-group",
      icon: <ShopOutlined />,
      label: "机构管理员",
      children: [
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
        },
      ],
    });
  }

  // 宠物信息维护员菜单
  if (roleSet.has("ORG_STAFF")) {
    items.push({
      key: "org-staff-group",
      icon: <MedicineBoxOutlined />,
      label: "宠物信息维护员",
      children: [
        {
          key: "/admin/pet-health",
          icon: <MedicineBoxOutlined />,
          label: "更新宠物健康信息",
        },
        {
          key: "/admin/feedback-archive",
          icon: <FileSearchOutlined />,
          label: "查看并归档用户反馈",
        },
      ],
    });
  }

  return items;
};

export default function AdminLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [openKeys, setOpenKeys] = useState([]);
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuthStore();
  const { message } = AntdApp.useApp();

  const roles = user?.roles || [];
  const menuItems = getMenuItems(roles, unreadCount);

  // 根据当前路径自动展开对应的菜单组
  useEffect(() => {
    const path = location.pathname;
    const groupKeys = [];

    // 检查路径属于哪个菜单组
    if (
      path.startsWith("/admin/role-management") ||
      path.startsWith("/admin/featured-content")
    ) {
      groupKeys.push("admin-group");
    } else if (
      path.startsWith("/admin/org-approval") ||
      path.startsWith("/admin/adoption-review")
    ) {
      groupKeys.push("auditor-group");
    } else if (
      path.startsWith("/admin/violation-review") ||
      path.startsWith("/admin/user-reminders") ||
      path.startsWith("/admin/user-appeals")
    ) {
      groupKeys.push("cs-group");
    } else if (
      path.startsWith("/admin/org-application") ||
      path.startsWith("/admin/org-staff") ||
      path.startsWith("/admin/pet-management") ||
      path.startsWith("/admin/adoption-approval") ||
      path.startsWith("/admin/interview-schedule") ||
      path.startsWith("/admin/adoption-confirm")
    ) {
      groupKeys.push("org-admin-group");
    } else if (
      path.startsWith("/admin/pet-health") ||
      path.startsWith("/admin/feedback-archive")
    ) {
      groupKeys.push("org-staff-group");
    }

    if (groupKeys.length > 0) {
      setOpenKeys(groupKeys);
    }
  }, [location.pathname]);

  useEffect(() => {
    // 加载未读消息数量
    loadUnreadCount();
    // 设置定时刷新
    const timer = setInterval(loadUnreadCount, 30000); // 每30秒刷新一次
    return () => clearInterval(timer);
  }, []);

  const loadUnreadCount = async () => {
    try {
      const res = await api.notification
        .getOrgSystemMessages()
        .catch(() => ({ data: [] }));
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
    // 只处理实际的路由路径，忽略菜单组的 key
    if (key && key.startsWith("/admin/")) {
      navigate(key);
    }
  };

  // 当侧边栏折叠时，关闭所有子菜单
  useEffect(() => {
    if (collapsed) {
      setOpenKeys([]);
    }
  }, [collapsed]);

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
          openKeys={openKeys}
          onOpenChange={setOpenKeys}
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
                    color:
                      location.pathname === "/admin/messages"
                        ? token.colorPrimary
                        : undefined,
                  }}
                  onClick={() => navigate("/admin/messages")}
                />
              </Badge>
            ) : (
              <MessageOutlined
                style={{
                  fontSize: 20,
                  cursor: "pointer",
                  color:
                    location.pathname === "/admin/messages"
                      ? token.colorPrimary
                      : undefined,
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
