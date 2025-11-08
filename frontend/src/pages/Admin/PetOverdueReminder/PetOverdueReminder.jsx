import React, { useState, useEffect } from "react";
import {
  Card,
  Table,
  Button,
  Space,
  Empty,
  Tag,
  InputNumber,
  App as AntdApp,
} from "antd";
import {
  ReloadOutlined,
  SendOutlined,
  WarningOutlined,
} from "@ant-design/icons";
import api from "../../../api";
import useAuthStore from "../../../store/authStore";

export default function PetOverdueReminder() {
  const { message } = AntdApp.useApp();
  const { user } = useAuthStore();
  const userId = user?.id || user?.userId;
  const [orgId, setOrgId] = useState(null);

  // 逾期宠物相关状态
  const [overduePets, setOverduePets] = useState([]);
  const [overdueLoading, setOverdueLoading] = useState(false);
  const [daysSinceUpdate, setDaysSinceUpdate] = useState(30);
  const [sendingReminder, setSendingReminder] = useState(false);

  // 获取用户所属的机构ID
  const fetchUserMemberships = async () => {
    if (!userId) {
      setOrgId(null);
      return;
    }
    try {
      const res = await api.org.getUserMemberships(userId);
      if (res?.code === 200) {
        const list = Array.isArray(res.data)
          ? res.data
          : res.data?.list || res.data?.memberships || [];
        const firstOrg = list
          .map((item) => {
            const org = item.organizationId ? item : item.org || item;
            return org.orgId || org.organizationId || org.id;
          })
          .filter(Boolean)[0];
        if (firstOrg) {
          setOrgId(firstOrg);
        } else {
          setOrgId(null);
        }
      } else {
        setOrgId(null);
      }
    } catch (e) {
      console.warn("获取用户机构列表失败", e);
      setOrgId(null);
    }
  };

  // 获取逾期未更新宠物列表
  const fetchOverduePets = async () => {
    if (!orgId) {
      setOverduePets([]);
      return;
    }
    setOverdueLoading(true);
    try {
      const res = await api.pets.getOverduePets(orgId, daysSinceUpdate);
      if (res?.code === 200) {
        setOverduePets(res.data || []);
      } else {
        message.error(res?.message || "获取逾期宠物列表失败");
        setOverduePets([]);
      }
    } catch (e) {
      message.error(e?.message || "获取逾期宠物列表失败");
      setOverduePets([]);
    } finally {
      setOverdueLoading(false);
    }
  };

  // 发送逾期提醒通知
  const handleSendReminder = async (pet) => {
    try {
      setSendingReminder(true);
      const res = await api.pets.sendOverdueReminder(
        pet.petId,
        pet.applicantId,
        pet.daysOverdue
      );
      if (res?.code === 200) {
        message.success("提醒通知已发送");
        // 刷新列表
        fetchOverduePets();
      } else {
        message.error(res?.message || "发送提醒失败");
      }
    } catch (e) {
      message.error(e?.message || "发送提醒失败");
    } finally {
      setSendingReminder(false);
    }
  };

  useEffect(() => {
    fetchUserMemberships();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  useEffect(() => {
    if (orgId) {
      fetchOverduePets();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orgId, daysSinceUpdate]);

  // 逾期宠物表格列
  const overdueColumns = [
    {
      title: "宠物ID",
      dataIndex: "petId",
      key: "petId",
      width: 100,
    },
    {
      title: "宠物名称",
      dataIndex: "petName",
      key: "petName",
      width: 150,
    },
    {
      title: "领养人ID",
      dataIndex: "applicantId",
      key: "applicantId",
      width: 120,
    },
    {
      title: "最后更新",
      dataIndex: "lastHealthUpdate",
      key: "lastHealthUpdate",
      width: 180,
      render: (text) => text || "从未更新",
    },
    {
      title: "逾期天数",
      dataIndex: "daysOverdue",
      key: "daysOverdue",
      width: 120,
      render: (days) => (
        <Tag color={days > 60 ? "red" : days > 30 ? "orange" : "default"}>
          {days} 天
        </Tag>
      ),
    },
    {
      title: "操作",
      key: "action",
      width: 150,
      render: (_, record) => (
        <Button
          type="primary"
          size="small"
          icon={<SendOutlined />}
          loading={sendingReminder}
          onClick={() => handleSendReminder(record)}
        >
          发送提醒
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Card
        title={
          <Space>
            <WarningOutlined />
            <span>逾期未更新健康状态的宠物</span>
          </Space>
        }
        extra={
          <Space>
            <span>逾期天数：</span>
            <InputNumber
              min={1}
              max={365}
              value={daysSinceUpdate}
              onChange={(value) => setDaysSinceUpdate(value || 30)}
              style={{ width: 100 }}
            />
            <span>天</span>
            <Button
              icon={<ReloadOutlined />}
              onClick={fetchOverduePets}
              loading={overdueLoading}
            >
              刷新
            </Button>
          </Space>
        }
      >
        <Table
          rowKey={(record) => `${record.petId}-${record.applicantId}`}
          columns={overdueColumns}
          dataSource={overduePets}
          loading={overdueLoading}
          locale={{ emptyText: <Empty description="暂无逾期未更新的宠物" /> }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
        />
      </Card>
    </div>
  );
}

