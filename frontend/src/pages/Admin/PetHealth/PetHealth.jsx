import React, { useState, useEffect, useCallback } from "react";
import {
  Card,
  Select,
  Form,
  InputNumber,
  Input,
  Button,
  Space,
  Empty,
  Divider,
  List,
  Tag,
  Typography,
  message,
} from "antd";
import { ReloadOutlined, CheckCircleOutlined } from "@ant-design/icons";
import api from "../../../api";
import useAuthStore from "../../../store/authStore";

const { Option } = Select;
const { Title, Text } = Typography;

const parseVaccine = (value) => {
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

export default function PetHealth() {
  const { user } = useAuthStore();
  const orgId = user?.orgId;

  const [form] = Form.useForm();
  const [petOptions, setPetOptions] = useState([]);
  const [selectedPetId, setSelectedPetId] = useState();
  const [health, setHealth] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const fetchPetOptions = useCallback(async () => {
    setLoading(true);
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
      setLoading(false);
    }
  }, [orgId, selectedPetId]);

  const fetchHealth = useCallback(
    async (petId) => {
      if (!petId) {
        setHealth(null);
        form.resetFields();
        return;
      }
      setLoading(true);
      try {
        const res = await api.pets.getPetHealth(petId);
        if (res?.code === 200) {
          const data = res.data;
          setHealth(data);
          form.setFieldsValue({
            weight: data?.weight ? Number(data.weight) : undefined,
            vaccine: parseVaccine(data?.vaccine),
            note: data?.note || "",
          });
        }
      } catch (error) {
        if (error?.code === 404) {
          setHealth(null);
          form.resetFields();
          message.info("该宠物暂无健康记录，请填写后提交");
        } else {
          message.error(error?.message || "获取健康信息失败");
        }
      } finally {
        setLoading(false);
      }
    },
    [form]
  );

  const fetchHistory = useCallback(async (petId) => {
    if (!petId) {
      setHistory([]);
      return;
    }
    setHistoryLoading(true);
    try {
      const res = await api.pets.getPetHealthHistory(petId);
      if (res?.code === 200) {
        setHistory(res.data || []);
      }
    } catch (error) {
      if (error?.code !== 404) {
        message.error(error?.message || "获取健康记录失败");
      } else {
        setHistory([]);
      }
    } finally {
      setHistoryLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPetOptions();
  }, [fetchPetOptions]);

  useEffect(() => {
    if (selectedPetId) {
      fetchHealth(selectedPetId);
      fetchHistory(selectedPetId);
    } else {
      form.resetFields();
      setHealth(null);
      setHistory([]);
    }
  }, [selectedPetId, fetchHealth, fetchHistory, form]);

  const handleSubmit = async () => {
    if (!selectedPetId) {
      message.warning("请先选择一只宠物");
      return;
    }

    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const payload = {
        weight: values.weight,
        vaccine: JSON.stringify(values.vaccine || []),
        note: values.note,
      };

      await api.pets.updatePetHealth(selectedPetId, payload);
      message.success("健康信息更新成功");
      fetchHealth(selectedPetId);
      fetchHistory(selectedPetId);
    } catch (error) {
      if (!error?.errorFields) {
        message.error(error?.message || "健康信息更新失败");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const selectedPet = petOptions.find((item) => item.id === selectedPetId);

  return (
    <div>
      <Card bordered={false} style={{ marginBottom: 16 }}>
        <Space direction="vertical" style={{ width: "100%" }} size={12}>
          <Space style={{ width: "100%", justifyContent: "space-between" }}>
            <Title level={2} style={{ margin: 0 }}>
              更新宠物健康信息
            </Title>
            <Space>
              <Button
                icon={<ReloadOutlined />}
                onClick={fetchPetOptions}
                loading={loading}
              >
                刷新列表
              </Button>
            </Space>
          </Space>
          <Text type="secondary">
            选择需要维护的宠物，填写体重、疫苗接种情况以及备注。提交后会自动记录更新时间并保留历史记录。
          </Text>
          <Select
            showSearch
            allowClear
            placeholder="请选择宠物"
            optionFilterProp="label"
            style={{ width: 320 }}
            value={selectedPetId}
            onChange={(value) => setSelectedPetId(value || undefined)}
            loading={loading}
            options={petOptions.map((pet) => ({
              value: pet.id,
              label: `${pet.name || "未命名"}（ID:${pet.id}）`,
            }))}
          />
          {selectedPet && (
            <Space size={16} wrap>
              <Tag color="blue">品种：{selectedPet.breed || "-"}</Tag>
              <Tag color="cyan">状态：{selectedPet.status}</Tag>
              <Tag color="green">更新时间：{selectedPet.updatedAt || "-"}</Tag>
            </Space>
          )}
        </Space>
      </Card>

      <Space direction="vertical" style={{ width: "100%" }} size={16}>
        <Card
          title="健康信息"
          bordered={false}
          loading={loading && !selectedPetId}
        >
          {selectedPetId ? (
            <Form form={form} layout="vertical">
              <Form.Item
                name="weight"
                label="体重 (kg)"
                rules={[{ required: true, message: "请输入体重" }]}
              >
                <InputNumber
                  min={0}
                  step={0.1}
                  style={{ width: "100%" }}
                  placeholder="例如：5.2"
                />
              </Form.Item>

              <Form.Item name="vaccine" label="疫苗接种">
                <Select
                  mode="tags"
                  tokenSeparators={[","]}
                  placeholder="请填写已接种疫苗，回车或逗号分隔"
                  allowClear
                />
              </Form.Item>

              <Form.Item name="note" label="健康备注">
                <Input.TextArea
                  rows={4}
                  maxLength={400}
                  placeholder="补充健康状况、喂养注意事项等"
                  showCount
                />
              </Form.Item>

              <Form.Item>
                <Space>
                  <Button
                    type="primary"
                    icon={<CheckCircleOutlined />}
                    loading={submitting}
                    onClick={handleSubmit}
                  >
                    保存更新
                  </Button>
                  <Button
                    onClick={() => {
                      if (selectedPetId) {
                        fetchHealth(selectedPetId);
                      }
                    }}
                  >
                    重置表单
                  </Button>
                </Space>
              </Form.Item>
            </Form>
          ) : (
            <Empty description="请选择宠物后再编辑健康信息" />
          )}
        </Card>

        <Card title="历史记录" bordered={false} loading={historyLoading}>
          {selectedPetId ? (
            history.length > 0 ? (
              <List
                dataSource={history}
                renderItem={(item) => (
                  <List.Item>
                    <Space
                      direction="vertical"
                      style={{ width: "100%" }}
                      size={4}
                    >
                      <Space split={<Divider type="vertical" />} wrap>
                        <Text strong>更新时间：{item.updatedAt}</Text>
                        <Text type="secondary">
                          操作人ID：{item.updatedBy || "-"}
                        </Text>
                        {item.weight && (
                          <Tag color="green">体重：{Number(item.weight)}kg</Tag>
                        )}
                      </Space>
                      <Space wrap>
                        {parseVaccine(item.vaccine).map((v) => (
                          <Tag key={v} color="blue">
                            {v}
                          </Tag>
                        ))}
                        {parseVaccine(item.vaccine).length === 0 && (
                          <Text type="secondary">无疫苗记录</Text>
                        )}
                      </Space>
                      {item.note && <Text>备注：{item.note}</Text>}
                    </Space>
                  </List.Item>
                )}
              />
            ) : (
              <Empty description="暂无历史记录" />
            )
          ) : (
            <Empty description="请选择宠物后查看历史记录" />
          )}
        </Card>
      </Space>
    </div>
  );
}
