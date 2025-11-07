import React, { useEffect, useState } from "react";
import {
  Card,
  Form,
  Input,
  Button,
  Typography,
  message,
  Descriptions,
  Space,
  Empty,
  Upload,
} from "antd";
import { UploadOutlined } from "@ant-design/icons";
import api from "../../../api";
import useAuthStore from "../../../store/authStore";

const { Title, Paragraph, Text } = Typography;

export default function OrgApplication() {
  const { user } = useAuthStore();
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [orgDetail, setOrgDetail] = useState(null);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [uploadingNew, setUploadingNew] = useState(false);
  const [uploadingUpdate, setUploadingUpdate] = useState(false);

  const orgId = user?.orgId;
  const userId = user?.id || user?.userId;

  const fetchOrgDetail = async () => {
    if (!orgId) {
      setOrgDetail(null);
      return;
    }
    setLoadingDetail(true);
    try {
      const res = await api.org.getOrgDetail(orgId);
      if (res?.code === 200) {
        setOrgDetail(res.data);
      } else {
        setOrgDetail(null);
      }
    } catch (e) {
      message.error(e?.message || "获取机构信息失败");
      setOrgDetail(null);
    } finally {
      setLoadingDetail(false);
    }
  };

  useEffect(() => {
    fetchOrgDetail();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orgId]);

  const handleSubmit = async (values) => {
    setSubmitting(true);
    try {
      const res = await api.org.applyOrg(values);
      if (res?.code === 200) {
        message.success("申请已提交，等待平台审核");
        form.resetFields();
      } else {
        message.error(res?.message || "提交失败");
      }
    } catch (e) {
      message.error(e?.message || "提交失败，请稍后重试");
    } finally {
      setSubmitting(false);
    }
  };

  const handleLicenseUpload = (targetOrgId, setLoading) => async (file) => {
    setLoading(true);
    try {
      const res = await api.org.uploadOrgLicense(file, targetOrgId);
      if (res?.code === 200) {
        message.success("资质文件上传成功");
        if (!targetOrgId) {
          const licenseUrl = res.data?.licenseUrl || res.data?.relativePath;
          form.setFieldsValue({ licenseUrl });
        } else {
          fetchOrgDetail();
        }
      } else {
        message.error(res?.message || "上传失败");
      }
    } catch (e) {
      message.error(e?.message || "上传失败，请稍后重试");
    } finally {
      setLoading(false);
    }
    return false;
  };

  return (
    <div>
      <Card style={{ marginBottom: 24 }}>
        <Title level={3} style={{ marginBottom: 12 }}>
          机构入驻申请
        </Title>
        <Paragraph type="secondary">
          提交真实、完整的机构资质信息，可加速审核通过。申请提交后，平台审核员将在
          3 个工作日内完成审核并以站内消息形式通知结果。
        </Paragraph>
      </Card>

      <Space direction="vertical" style={{ width: "100%" }} size={24}>
        <Card title="我所在的机构" loading={loadingDetail}>
          {orgId ? (
            orgDetail ? (
              <Space direction="vertical" style={{ width: "100%" }} size={16}>
                <Descriptions size="small" column={1} bordered>
                  <Descriptions.Item label="机构ID">{orgId}</Descriptions.Item>
                  <Descriptions.Item label="机构名称">{orgDetail.name || "-"}</Descriptions.Item>
                  <Descriptions.Item label="地址">{orgDetail.address || "-"}</Descriptions.Item>
                  <Descriptions.Item label="联系人">{orgDetail.contactName || "-"}</Descriptions.Item>
                  <Descriptions.Item label="联系电话">{orgDetail.contactPhone || "-"}</Descriptions.Item>
                  <Descriptions.Item label="资质地址">{orgDetail.licenseUrl || "-"}</Descriptions.Item>
                  <Descriptions.Item label="状态">{orgDetail.status || "-"}</Descriptions.Item>
                </Descriptions>
                <Upload
                  beforeUpload={handleLicenseUpload(orgId, setUploadingUpdate)}
                  showUploadList={false}
                  accept="image/*"
                >
                  <Button icon={<UploadOutlined />} loading={uploadingUpdate}>
                    更新资质文件
                  </Button>
                </Upload>
                <Text type="secondary">更新后平台将重新校验资质信息。</Text>
              </Space>
            ) : (
              <Empty description="暂无机构详情，请稍后刷新" />
            )
          ) : (
            <Text>当前账号尚未关联机构，可通过以下表单提交入驻申请。</Text>
          )}
        </Card>

        <Card title="提交新的入驻申请">
          <Form
            layout="vertical"
            form={form}
            style={{ maxWidth: 600 }}
            onFinish={handleSubmit}
          >
            <Form.Item
              label="机构名称"
              name="name"
              rules={[{ required: true, message: "请输入机构名称" }]}
            >
              <Input placeholder="例如：城市流浪动物救助中心" allowClear />
            </Form.Item>
            <Form.Item
              label="资质证明URL"
              name="licenseUrl"
              rules={[{ required: true, message: "请输入资质文件的URL" }]}
              extra="请先完成资质文件上传后填写对应访问地址"
            >
              <Input
                placeholder="例如：https://cdn.example.com/license/xxx.jpg"
                allowClear
              />
            </Form.Item>
            <Form.Item label="上传资质文件">
              <Upload
                beforeUpload={handleLicenseUpload(null, setUploadingNew)}
                showUploadList={false}
                accept="image/*"
              >
                <Button icon={<UploadOutlined />} loading={uploadingNew}>
                  上传图片并自动填写链接
                </Button>
              </Upload>
              <Text type="secondary">支持 JPG/PNG 等格式，上传成功后自动填充链接。</Text>
            </Form.Item>
            <Form.Item
              label="机构地址"
              name="address"
              rules={[{ required: true, message: "请输入机构地址" }]}
            >
              <Input placeholder="省市区 + 详细街道" allowClear />
            </Form.Item>
            <Form.Item
              label="联系人"
              name="contactName"
              rules={[{ required: true, message: "请输入联系人" }]}
            >
              <Input allowClear />
            </Form.Item>
            <Form.Item
              label="联系电话"
              name="contactPhone"
              rules={[{ required: true, message: "请输入联系电话" }]}
            >
              <Input allowClear />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={submitting}>
                提交申请
              </Button>
            </Form.Item>
          </Form>
          {!userId && (
            <Paragraph type="warning" style={{ marginTop: 12 }}>
              当前未获取到用户ID，提交申请可能失败，请尝试刷新页面或重新登录。
            </Paragraph>
          )}
        </Card>
      </Space>
    </div>
  );
}
