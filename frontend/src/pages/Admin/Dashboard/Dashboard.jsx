import React from "react";
import { Card, Row, Col, Statistic, Typography } from "antd";
import {
  UserOutlined,
  HeartOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  ClockCircleOutlined,
  TeamOutlined,
  CalendarOutlined,
  MedicineBoxOutlined,
  FileSearchOutlined,
} from "@ant-design/icons";
import useAuthStore from "../../../store/authStore";

const { Title } = Typography;

export default function Dashboard() {
  const { user } = useAuthStore();
  const roles = user?.roles || [];
  const roleSet = new Set(roles);

  // 根据角色决定显示的统计卡片
  const getStatistics = () => {
    const stats = [];

    // 超级管理员 - 全局数据
    if (roleSet.has("ADMIN")) {
      stats.push(
        {
          title: "待审核机构",
          value: 0,
          icon: <FileTextOutlined />,
          color: "#1890ff",
        },
        {
          title: "待审核申请",
          value: 0,
          icon: <CheckCircleOutlined />,
          color: "#52c41a",
        },
        {
          title: "待处理申诉",
          value: 0,
          icon: <UserOutlined />,
          color: "#faad14",
        },
        {
          title: "在线宠物总数",
          value: 0,
          icon: <HeartOutlined />,
          color: "#f5222d",
        },
        {
          title: "平台用户总数",
          value: 0,
          icon: <TeamOutlined />,
          color: "#722ed1",
        }
      );
    }

    // 审核员 - 审核相关数据
    if (roleSet.has("AUDITOR")) {
      stats.push(
        {
          title: "待审核机构入驻",
          value: 0,
          icon: <FileTextOutlined />,
          color: "#1890ff",
        },
        {
          title: "待审核领养申请（复审）",
          value: 0,
          icon: <CheckCircleOutlined />,
          color: "#52c41a",
        }
      );
    }

    // 客服人员 - 客服相关数据
    if (roleSet.has("CS")) {
      stats.push(
        {
          title: "待复核违规内容",
          value: 0,
          icon: <WarningOutlined />,
          color: "#faad14",
        },
        {
          title: "待处理用户申诉",
          value: 0,
          icon: <UserOutlined />,
          color: "#f5222d",
        },
        {
          title: "需提醒更新状态",
          value: 0,
          icon: <ClockCircleOutlined />,
          color: "#1890ff",
        }
      );
    }

    // 机构管理员 - 机构相关数据
    if (roleSet.has("ORG_ADMIN")) {
      stats.push(
        {
          title: "待审核用户申请（初审）",
          value: 0,
          icon: <CheckCircleOutlined />,
          color: "#52c41a",
        },
        {
          title: "待安排面谈",
          value: 0,
          icon: <CalendarOutlined />,
          color: "#1890ff",
        },
        {
          title: "待确认领养交接",
          value: 0,
          icon: <FileTextOutlined />,
          color: "#faad14",
        },
        {
          title: "机构宠物总数",
          value: 0,
          icon: <HeartOutlined />,
          color: "#f5222d",
        }
      );
    }

    // 宠物信息维护员 - 宠物相关数据
    if (roleSet.has("ORG_STAFF")) {
      stats.push(
        {
          title: "待更新健康信息",
          value: 0,
          icon: <MedicineBoxOutlined />,
          color: "#52c41a",
        },
        {
          title: "待归档用户反馈",
          value: 0,
          icon: <FileSearchOutlined />,
          color: "#1890ff",
        },
        {
          title: "机构宠物总数",
          value: 0,
          icon: <HeartOutlined />,
          color: "#f5222d",
        }
      );
    }

    // 如果没有匹配的角色，显示默认数据
    if (stats.length === 0) {
      stats.push({
        title: "欢迎使用管理后台",
        value: 0,
        icon: <UserOutlined />,
        color: "#1890ff",
      });
    }

    return stats;
  };

  const statistics = getStatistics();

  // 根据角色获取欢迎标题
  const getWelcomeTitle = () => {
    if (roleSet.has("ADMIN")) {
      return "超级管理员 - 管理后台首页";
    } else if (roleSet.has("AUDITOR")) {
      return "审核员 - 管理后台首页";
    } else if (roleSet.has("CS")) {
      return "客服人员 - 管理后台首页";
    } else if (roleSet.has("ORG_ADMIN")) {
      return "机构管理员 - 管理后台首页";
    } else if (roleSet.has("ORG_STAFF")) {
      return "宠物信息维护员 - 管理后台首页";
    }
    return "管理后台首页";
  };

  return (
    <div>
      <Title level={2}>{getWelcomeTitle()}</Title>
      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        {statistics.map((stat, index) => (
          <Col xs={24} sm={12} lg={6} key={index}>
            <Card>
              <Statistic
                title={stat.title}
                value={stat.value}
                prefix={stat.icon}
                valueStyle={{ color: stat.color }}
              />
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
}
