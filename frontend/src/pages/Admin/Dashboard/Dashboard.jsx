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
    // Adoption 相关统计数据
    pendingAdoptionApps: 0, // 待审核用户申请（初审）- PENDING
    pendingAdoptionAppsLoading: false,
    pendingReviewApps: 0, // 待审核领养申请（复审）- ORG_APPROVED
    pendingReviewAppsLoading: false,
    pendingHandoverApps: 0, // 待确认领养交接 - PLATFORM_APPROVED
    pendingHandoverAppsLoading: false,
    // Interview 相关统计数据
    pendingInterviewRequests: 0, // 待安排面谈 - 状态为 REQUESTED
    pendingInterviewRequestsLoading: false,
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

  // 获取待审核用户申请（初审）- 机构管理员
  useEffect(() => {
    if (!selectedOrgId || !roleSet.has("ORG_ADMIN")) {
      setStatsData((prev) => ({ ...prev, pendingAdoptionApps: 0 }));
      return;
    }
    const fetchPendingAdoptionApps = async () => {
      setStatsData((prev) => ({ ...prev, pendingAdoptionAppsLoading: true }));
      try {
        const res = await api.adoption.getPendingApplications("PENDING");
        if (res?.code === 200) {
          const list = Array.isArray(res.data)
            ? res.data
            : res.data?.list || [];
          setStatsData((prev) => ({
            ...prev,
            pendingAdoptionApps: list.length,
          }));
        }
      } catch (e) {
        console.warn("获取待审核用户申请失败", e);
      } finally {
        setStatsData((prev) => ({
          ...prev,
          pendingAdoptionAppsLoading: false,
        }));
      }
    };
    fetchPendingAdoptionApps();
  }, [selectedOrgId, roleSet]);

  // 获取待审核领养申请（复审）- 审核员
  useEffect(() => {
    if (!roleSet.has("AUDITOR")) {
      setStatsData((prev) => ({ ...prev, pendingReviewApps: 0 }));
      return;
    }
    const fetchPendingReviewApps = async () => {
      setStatsData((prev) => ({ ...prev, pendingReviewAppsLoading: true }));
      try {
        const res = await api.adoption.getPendingApplications("ORG_APPROVED");
        if (res?.code === 200) {
          const list = Array.isArray(res.data)
            ? res.data
            : res.data?.list || [];
          setStatsData((prev) => ({
            ...prev,
            pendingReviewApps: list.length,
          }));
        }
      } catch (e) {
        console.warn("获取待审核领养申请（复审）失败", e);
      } finally {
        setStatsData((prev) => ({
          ...prev,
          pendingReviewAppsLoading: false,
        }));
      }
    };
    fetchPendingReviewApps();
  }, [roleSet]);

  // 获取待安排面谈 - 机构管理员
  useEffect(() => {
    if (!selectedOrgId || !roleSet.has("ORG_ADMIN")) {
      setStatsData((prev) => ({ ...prev, pendingInterviewRequests: 0 }));
      return;
    }
    const fetchPendingInterviewRequests = async () => {
      setStatsData((prev) => ({
        ...prev,
        pendingInterviewRequestsLoading: true,
      }));
      try {
        // 获取平台已通过的申请
        const res = await api.adoption.getPendingApplications(
          "PLATFORM_APPROVED"
        );
        if (res?.code === 200) {
          const list = Array.isArray(res.data)
            ? res.data
            : res.data?.list || [];
          // 为每个申请获取面谈信息，统计状态为 REQUESTED 的数量
          let count = 0;
          await Promise.all(
            list.map(async (app) => {
              try {
                const interviewRes = await api.interview.getInterviewRequest(
                  app.id
                );
                if (
                  interviewRes?.code === 200 &&
                  interviewRes.data?.status === "REQUESTED"
                ) {
                  count++;
                }
              } catch {
                // 如果没有面谈预约，忽略
              }
            })
          );
          setStatsData((prev) => ({
            ...prev,
            pendingInterviewRequests: count,
          }));
        }
      } catch (e) {
        console.warn("获取待安排面谈失败", e);
      } finally {
        setStatsData((prev) => ({
          ...prev,
          pendingInterviewRequestsLoading: false,
        }));
      }
    };
    fetchPendingInterviewRequests();
  }, [selectedOrgId, roleSet]);

  // 获取待确认领养交接 - 机构管理员
  useEffect(() => {
    if (!selectedOrgId || !roleSet.has("ORG_ADMIN")) {
      setStatsData((prev) => ({ ...prev, pendingHandoverApps: 0 }));
      return;
    }
    const fetchPendingHandoverApps = async () => {
      setStatsData((prev) => ({ ...prev, pendingHandoverAppsLoading: true }));
      try {
        const res = await api.adoption.getPendingApplications(
          "PLATFORM_APPROVED"
        );
        if (res?.code === 200) {
          const list = Array.isArray(res.data)
            ? res.data
            : res.data?.list || [];
          // 为每个申请获取面谈信息，只统计已完成面谈的申请
          let count = 0;
          await Promise.all(
            list.map(async (app) => {
              try {
                const interviewRes = await api.interview.getInterviewRequest(
                  app.id
                );
                if (
                  interviewRes?.code === 200 &&
                  interviewRes.data?.status === "DONE"
                ) {
                  count++;
                }
              } catch {
                // 如果没有面谈预约或面谈未完成，不统计
              }
            })
          );
          setStatsData((prev) => ({
            ...prev,
            pendingHandoverApps: count,
          }));
        }
      } catch (e) {
        console.warn("获取待确认领养交接失败", e);
      } finally {
        setStatsData((prev) => ({
          ...prev,
          pendingHandoverAppsLoading: false,
        }));
      }
    };
    fetchPendingHandoverApps();
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
          value: statsData.pendingReviewApps,
          icon: <CheckCircleOutlined />,
          color: "#52c41a",
          loading: statsData.pendingReviewAppsLoading,
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
          value: statsData.pendingReviewApps,
          icon: <CheckCircleOutlined />,
          color: "#52c41a",
          loading: statsData.pendingReviewAppsLoading,
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
          value: statsData.pendingAdoptionApps,
          icon: <CheckCircleOutlined />,
          color: "#52c41a",
          loading: statsData.pendingAdoptionAppsLoading,
        },
        {
          title: "待安排面谈",
          value: statsData.pendingInterviewRequests,
          icon: <CalendarOutlined />,
          color: "#1890ff",
          loading: statsData.pendingInterviewRequestsLoading,
        },
        {
          title: "待确认领养交接",
          value: statsData.pendingHandoverApps,
          icon: <FileTextOutlined />,
          color: "#faad14",
          loading: statsData.pendingHandoverAppsLoading,
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
