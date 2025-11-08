import React, { useState, useEffect, useCallback } from "react";
import {
  Card,
  Select,
  Space,
  Button,
  Typography,
  List,
  Tag,
  Empty,
  App as AntdApp,
} from "antd";
import { ReloadOutlined, FileTextOutlined, LinkOutlined } from "@ant-design/icons";
import api from "../../../api";
import useAuthStore from "../../../store/authStore";

const { Option } = Select;
const { Title, Text, Paragraph } = Typography;

const parseMediaUrls = (value) => {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) {
      return parsed.filter(Boolean);
    }
    return [];
  } catch (error) {
    return [];
  }
};

export default function FeedbackArchive() {
  const { message } = AntdApp.useApp();
  const { user } = useAuthStore();
  const orgId = user?.orgId;

  const [petOptions, setPetOptions] = useState([]);
  const [selectedPetId, setSelectedPetId] = useState();
  const [feedbacks, setFeedbacks] = useState([]);
  const [loadingPets, setLoadingPets] = useState(false);
  const [loadingFeedback, setLoadingFeedback] = useState(false);

  const fetchPetOptions = useCallback(async () => {
    setLoadingPets(true);
    try {
      const res = await api.pets.fetchPets({ page: 1, pageSize: 200, orgId });
      if (res?.code === 200) {
        const { list = [] } = res.data || {};
        setPetOptions(list);
        if (!selectedPetId && list.length > 0) {
          setSelectedPetId(list[0].id);
        }
      }
    } catch (error) {
      message.error(error?.message || "获取宠物列表失败");
    } finally {
      setLoadingPets(false);
    }
  }, [orgId, selectedPetId]);

  const fetchFeedbacks = useCallback(
    async (petId) => {
      if (!petId) {
        setFeedbacks([]);
        return;
      }
      setLoadingFeedback(true);
      try {
        const res = await api.pets.getPetFeedbacks(petId);
        if (res?.code === 200) {
          setFeedbacks(res.data || []);
        }
      } catch (error) {
        if (error?.code === 404) {
          setFeedbacks([]);
          message.info("暂无用户反馈");
        } else {
          message.error(error?.message || "获取反馈失败");
        }
      } finally {
        setLoadingFeedback(false);
      }
    },
    []
  );

  useEffect(() => {
    fetchPetOptions();
  }, [fetchPetOptions]);

  useEffect(() => {
    if (selectedPetId) {
      fetchFeedbacks(selectedPetId);
    } else {
      setFeedbacks([]);
    }
  }, [selectedPetId, fetchFeedbacks]);

  const selectedPet = petOptions.find((item) => item.id === selectedPetId);

  return (
    <div>
      <Card bordered={false} style={{ marginBottom: 16 }}>
        <Space direction="vertical" style={{ width: "100%" }} size={12}>
          <Space style={{ width: "100%", justifyContent: "space-between" }}>
            <Title level={2} style={{ margin: 0 }}>
              查看并归档用户反馈
            </Title>
            <Space>
              <Button icon={<ReloadOutlined />} loading={loadingPets} onClick={fetchPetOptions}>
                刷新列表
              </Button>
              {selectedPetId && (
                <Button icon={<ReloadOutlined />} loading={loadingFeedback} onClick={() => fetchFeedbacks(selectedPetId)}>
                  刷新反馈
                </Button>
              )}
            </Space>
          </Space>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            维护员可查看用户提交的领养后续反馈，支持查看文字内容与附件链接，可用于归档优秀案例或跟踪问题。
          </Paragraph>
          <Select
            showSearch
            allowClear
            placeholder="请选择宠物"
            optionFilterProp="label"
            style={{ width: 320 }}
            value={selectedPetId}
            loading={loadingPets}
            onChange={(value) => setSelectedPetId(value || undefined)}
            options={petOptions.map((pet) => ({
              value: pet.id,
              label: `${pet.name || "未命名"}（ID:${pet.id}）`,
            }))}
          />
          {selectedPet && (
            <Space size={16} wrap>
              <Tag color="blue">类型：{selectedPet.type}</Tag>
              <Tag color="green">状态：{selectedPet.status}</Tag>
              <Tag color="purple">更新时间：{selectedPet.updatedAt || "-"}</Tag>
            </Space>
          )}
        </Space>
      </Card>

      <Card bordered={false} title="用户反馈列表" loading={loadingFeedback}>
        {selectedPetId ? (
          feedbacks.length > 0 ? (
            <List
              dataSource={feedbacks}
              renderItem={(item) => {
                const mediaList = parseMediaUrls(item.mediaUrls);
                return (
                  <List.Item>
                    <Space direction="vertical" style={{ width: "100%" }} size={6}>
                      <Space split={<Tag color="default">|</Tag>} wrap>
                        <Tag color="geekblue">反馈ID：{item.id}</Tag>
                        <Tag color="green">用户ID：{item.userId}</Tag>
                        <Tag color="orange">提交时间：{item.createdAt}</Tag>
                      </Space>
                      <Space>
                        <FileTextOutlined style={{ color: "#1890ff" }} />
                        <Text>{item.content || "（无文字内容）"}</Text>
                      </Space>
                      {mediaList.length > 0 && (
                        <Space direction="vertical" size={4}>
                          <Text type="secondary">附件：</Text>
                          <Space direction="vertical" size={2}>
                            {mediaList.map((url, index) => (
                              <a key={url} href={url} target="_blank" rel="noreferrer">
                                <Space size={4}>
                                  <LinkOutlined />
                                  <span>{`附件 ${index + 1}`}</span>
                                </Space>
                              </a>
                            ))}
                          </Space>
                        </Space>
                      )}
                      {mediaList.length === 0 && <Text type="secondary">无附件</Text>}
                    </Space>
                  </List.Item>
                );
              }}
            />
          ) : (
            <Empty description="暂无用户反馈" />
          )
        ) : (
          <Empty description="请选择宠物后查看反馈" />
        )}
      </Card>
    </div>
  );
}

