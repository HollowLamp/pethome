import React, { useEffect, useMemo, useState } from "react";
import { Card, Row, Col, Statistic, Typography, Select, Space } from "antd";
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
import api from "../../../api";

const { Title } = Typography;

export default function Dashboard() {
  const { user } = useAuthStore();
  const userId = user?.id || user?.userId;

  const [roles, setRoles] = useState([]);
  const roleSet = useMemo(() => new Set(roles), [roles]);
  const [orgList, setOrgList] = useState([]);
  const [selectedOrgId, setSelectedOrgId] = useState(null);
  const [orgListLoading, setOrgListLoading] = useState(false);

  const [orgMembersCount, setOrgMembersCount] = useState(0);
  const [orgMembersLoading, setOrgMembersLoading] = useState(false);
  const [statsData, setStatsData] = useState({
    pendingOrgs: 0,
    pendingOrgsLoading: false,
    availablePets: 0,
    availablePetsLoading: false,
    totalUsers: 0,
    totalUsersLoading: false,
    orgPetsCount: 0,
    orgPetsLoading: false,
  });

  // 获取用户角色
  useEffect(() => {
    if (!userId) {
      setRoles([]);
      return;
    }
    const fetchUserRoles = async () => {
      try {
        const res = await api.auth.getUserRolesApi(userId);
        if (res?.code === 200) {
          const roleList = Array.isArray(res.data) ? res.data : [];
          setRoles(roleList);
        }
      } catch (e) {
        console.warn("获取用户角色失败", e);
        setRoles([]);
      }
    };
    fetchUserRoles();
  }, [userId]);

  // 获取用户所属机构列表
  useEffect(() => {
    if (!userId) {
      setOrgList([]);
      setSelectedOrgId(null);
      return;
    }
    // 只有机构相关角色才需要获取机构列表
    if (!roleSet.has("ORG_ADMIN") && !roleSet.has("ORG_STAFF")) {
      setOrgList([]);
      setSelectedOrgId(null);
      return;
    }
    const fetchUserMemberships = async () => {
      setOrgListLoading(true);
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
                id,
                name: org.name || `机构 ${id}`,
              };
            })
            .filter(Boolean);
          setOrgList(mapped);
          if (mapped.length > 0) {
            setSelectedOrgId((prev) => prev ?? mapped[0].id);
          } else {
            setSelectedOrgId(null);
          }
        }
      } catch (e) {
        console.warn("获取用户机构列表失败", e);
        setOrgList([]);
        setSelectedOrgId(null);
      } finally {
        setOrgListLoading(false);
      }
    };
    fetchUserMemberships();
  }, [userId, roleSet]);

  // 获取待审核机构数量
  useEffect(() => {
    if (!roleSet.has("ADMIN") && !roleSet.has("AUDITOR")) return;
    const fetchPendingOrgs = async () => {
      setStatsData((prev) => ({ ...prev, pendingOrgsLoading: true }));
      try {
        const res = await api.org.getPendingOrganizations();
        if (res?.code === 200) {
          const list = Array.isArray(res.data)
            ? res.data
            : res.data?.list || [];
          setStatsData((prev) => ({ ...prev, pendingOrgs: list.length }));
        }
      } catch (e) {
        console.warn("获取待审核机构失败", e);
      } finally {
        setStatsData((prev) => ({ ...prev, pendingOrgsLoading: false }));
      }
    };
    fetchPendingOrgs();
  }, [roleSet]);

  // 获取在线宠物总数（超级管理员）
  useEffect(() => {
    if (!roleSet.has("ADMIN")) return;
    const fetchAvailablePets = async () => {
      setStatsData((prev) => ({ ...prev, availablePetsLoading: true }));
      try {
        const res = await api.pets.fetchPets({
          status: "AVAILABLE",
          page: 1,
          pageSize: 1,
        });
        if (res?.code === 200) {
          const total = res.data?.total || 0;
          setStatsData((prev) => ({ ...prev, availablePets: total }));
        }
      } catch (e) {
        console.warn("获取在线宠物总数失败", e);
      } finally {
        setStatsData((prev) => ({ ...prev, availablePetsLoading: false }));
      }
    };
    fetchAvailablePets();
  }, [roleSet]);

  // 获取平台用户总数（超级管理员）
  useEffect(() => {
    if (!roleSet.has("ADMIN")) return;
    const fetchTotalUsers = async () => {
      setStatsData((prev) => ({ ...prev, totalUsersLoading: true }));
      try {
        const res = await api.auth.getUserListApi({ page: 1, pageSize: 1 });
        if (res?.code === 200) {
          const total = res.data?.total || 0;
          setStatsData((prev) => ({ ...prev, totalUsers: total }));
        }
      } catch (e) {
        console.warn("获取平台用户总数失败", e);
      } finally {
        setStatsData((prev) => ({ ...prev, totalUsersLoading: false }));
      }
    };
    fetchTotalUsers();
  }, [roleSet]);

  // 获取机构宠物总数（机构管理员、宠物信息维护员）
  useEffect(() => {
    if (!selectedOrgId) {
      setStatsData((prev) => ({ ...prev, orgPetsCount: 0 }));
      return;
    }
    if (!roleSet.has("ORG_ADMIN") && !roleSet.has("ORG_STAFF")) return;
    const fetchOrgPets = async () => {
      setStatsData((prev) => ({ ...prev, orgPetsLoading: true }));
      try {
        const res = await api.pets.fetchPets({
          orgId: selectedOrgId,
          page: 1,
          pageSize: 1,
        });
        if (res?.code === 200) {
          const total = res.data?.total || 0;
          setStatsData((prev) => ({ ...prev, orgPetsCount: total }));
        }
      } catch (e) {
        console.warn("获取机构宠物总数失败", e);
      } finally {
        setStatsData((prev) => ({ ...prev, orgPetsLoading: false }));
      }
    };
    fetchOrgPets();
  }, [selectedOrgId, roleSet]);

  useEffect(() => {
    if (!selectedOrgId) {
      setOrgMembersCount(0);
      return;
    }
    if (!roleSet.has("ORG_ADMIN") && !roleSet.has("ORG_STAFF")) return;

    const fetchMembers = async () => {
      setOrgMembersLoading(true);
      try {
        const res = await api.org.getOrgMembers(selectedOrgId);
        if (res?.code === 200) {
          const list = Array.isArray(res.data)
            ? res.data
            : res.data?.list || [];
          setOrgMembersCount(list.length);
        }
      } catch (e) {
        console.warn("加载机构成员失败", e);
      } finally {
        setOrgMembersLoading(false);
      }
    };

    fetchMembers();
  }, [selectedOrgId, roleSet]);

  const statistics = useMemo(() => {
    const stats = [];

    // 超级管理员 - 全局数据
    if (roleSet.has("ADMIN")) {
      stats.push(
        {
          title: "待审核机构",
          value: statsData.pendingOrgs,
          icon: <FileTextOutlined />,
          color: "#1890ff",
          loading: statsData.pendingOrgsLoading,
        },
        {
          title: "待审核申请",
          value: 0,
          icon: <CheckCircleOutlined />,
          color: "#52c41a",
          // TODO: 待开发 - 领养申请审核接口
        },
        {
          title: "待处理申诉",
          value: 0,
          icon: <UserOutlined />,
          color: "#faad14",
          // TODO: 待开发 - 申诉处理接口
        },
        {
          title: "在线宠物总数",
          value: statsData.availablePets,
          icon: <HeartOutlined />,
          color: "#f5222d",
          loading: statsData.availablePetsLoading,
        },
        {
          title: "平台用户总数",
          value: statsData.totalUsers,
          icon: <TeamOutlined />,
          color: "#722ed1",
          loading: statsData.totalUsersLoading,
        }
      );
    }

    // 审核员 - 审核相关数据
    if (roleSet.has("AUDITOR")) {
      stats.push(
        {
          title: "待审核机构入驻",
          value: statsData.pendingOrgs,
          icon: <FileTextOutlined />,
          color: "#1890ff",
          loading: statsData.pendingOrgsLoading,
        },
        {
          title: "待审核领养申请（复审）",
          value: 0,
          icon: <CheckCircleOutlined />,
          color: "#52c41a",
          // TODO: 待开发 - 领养申请复审接口
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
          // TODO: 待开发 - 违规内容复核接口
        },
        {
          title: "待处理用户申诉",
          value: 0,
          icon: <UserOutlined />,
          color: "#f5222d",
          // TODO: 待开发 - 用户申诉处理接口
        },
        {
          title: "需提醒更新状态",
          value: 0,
          icon: <ClockCircleOutlined />,
          color: "#1890ff",
          // TODO: 待开发 - 状态更新提醒接口
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
          // TODO: 待开发 - 用户申请初审接口
        },
        {
          title: "待安排面谈",
          value: 0,
          icon: <CalendarOutlined />,
          color: "#1890ff",
          // TODO: 待开发 - 面谈安排接口
        },
        {
          title: "待确认领养交接",
          value: 0,
          icon: <FileTextOutlined />,
          color: "#faad14",
          // TODO: 待开发 - 领养交接确认接口
        },
        {
          title: "机构成员总数",
          value: orgMembersCount,
          icon: <TeamOutlined />,
          color: "#1890ff",
          loading: orgMembersLoading,
        },
        {
          title: "机构宠物总数",
          value: statsData.orgPetsCount,
          icon: <HeartOutlined />,
          color: "#f5222d",
          loading: statsData.orgPetsLoading,
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
          // TODO: 待开发 - 健康信息更新提醒接口
        },
        {
          title: "待归档用户反馈",
          value: 0,
          icon: <FileSearchOutlined />,
          color: "#1890ff",
          // TODO: 待开发 - 用户反馈归档接口
        },
        {
          title: "机构成员总数",
          value: orgMembersCount,
          icon: <TeamOutlined />,
          color: "#1890ff",
          loading: orgMembersLoading,
        },
        {
          title: "机构宠物总数",
          value: statsData.orgPetsCount,
          icon: <HeartOutlined />,
          color: "#f5222d",
          loading: statsData.orgPetsLoading,
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
  }, [roleSet, orgMembersCount, orgMembersLoading, statsData]);

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
      <Space
        style={{
          width: "100%",
          justifyContent: "space-between",
          marginBottom: 16,
        }}
      >
        <Title level={2} style={{ margin: 0 }}>
          {getWelcomeTitle()}
        </Title>
        {orgList.length > 1 && (
          <Select
            placeholder="选择机构"
            loading={orgListLoading}
            value={selectedOrgId}
            style={{ width: 220 }}
            onChange={(value) => setSelectedOrgId(value)}
            options={orgList.map((org) => ({
              label: org.name,
              value: org.id,
            }))}
          />
        )}
        {orgList.length === 1 && (
          <Typography.Text type="secondary">
            机构：{orgList[0].name}
          </Typography.Text>
        )}
      </Space>
      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        {statistics.map((stat, index) => (
          <Col xs={24} sm={12} lg={6} key={index}>
            <Card>
              <Statistic
                title={stat.title}
                value={stat.value}
                prefix={stat.icon}
                valueStyle={{ color: stat.color }}
                loading={stat.loading}
              />
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
}
