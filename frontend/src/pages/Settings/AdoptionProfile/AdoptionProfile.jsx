import React, { useEffect, useState } from "react";
import {
  Card,
  Form,
  Upload,
  Button,
  Typography,
  Space,
  List,
  Tag,
  Modal,
  Empty,
  Divider,
  App as AntdApp,
} from "antd";
import {
  UploadOutlined,
  DeleteOutlined,
  FileTextOutlined,
  EyeOutlined,
} from "@ant-design/icons";
import api from "../../../api";
import useAuthStore from "../../../store/authStore";

const { Title, Paragraph, Text } = Typography;

// 材料类型选项
const DOC_TYPE_OPTIONS = [
  { value: "ID_CARD", label: "身份证", required: true },
  { value: "INCOME_PROOF", label: "收入证明", required: false },
  { value: "PET_HISTORY", label: "养宠历史", required: false },
  { value: "HOUSING_PROOF", label: "住房证明", required: false },
  { value: "OTHER", label: "其他材料", required: false },
];

const DOC_TYPE_LABELS = DOC_TYPE_OPTIONS.reduce((acc, item) => {
  acc[item.value] = item.label;
  return acc;
}, {});

export default function AdoptionProfile() {
  const { message, modal } = AntdApp.useApp();
  const { isLoggedIn, user } = useAuthStore();
  const [profileDocs, setProfileDocs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [previewVisible, setPreviewVisible] = useState(false);
  const [previewUrl, setPreviewUrl] = useState("");

  const fetchProfile = async () => {
    if (!isLoggedIn) {
      setProfileDocs([]);
      return;
    }
    setLoading(true);
    try {
      const res = await api.adoption.getUserProfile();
      if (res?.code === 200) {
        setProfileDocs(Array.isArray(res.data) ? res.data : []);
      }
    } catch (e) {
      message.error(e?.message || "获取领养资料失败");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, [isLoggedIn]);

  const handleUpload = async (file, docType) => {
    setUploading(true);
    try {
      const res = await api.adoption.uploadProfileDoc(file, docType);
      if (res?.code === 200) {
        message.success("材料上传成功");
        fetchProfile();
      } else {
        message.error(res?.message || "上传失败");
      }
    } catch (e) {
      message.error(e?.message || "上传失败，请稍后重试");
    } finally {
      setUploading(false);
    }
    return false; // 阻止默认上传行为
  };

  const handleDelete = async (docId) => {
    modal.confirm({
      title: "确认删除",
      content: "确定要删除这个材料吗？",
      onOk: async () => {
        try {
          const res = await api.adoption.deleteProfileDoc(docId);
          if (res?.code === 200) {
            message.success("材料已删除");
            fetchProfile();
          } else {
            message.error(res?.message || "删除失败");
          }
        } catch (e) {
          message.error(e?.message || "删除失败，请稍后重试");
        }
      },
    });
  };

  const handlePreview = (url) => {
    const fullUrl = url.startsWith("http") ? url : `/files/${url}`;
    setPreviewUrl(fullUrl);
    setPreviewVisible(true);
  };

  const getDocByType = (docType) => {
    return profileDocs.find((doc) => doc.docType === docType);
  };

  if (!isLoggedIn) {
    return (
      <div style={{ padding: 24, textAlign: "center" }}>
        <Card>
          <Empty
            description="请先登录后填写领养资料"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button
              type="primary"
              onClick={() =>
                window.dispatchEvent(new Event("OPEN_LOGIN_MODAL"))
              }
            >
              去登录
            </Button>
          </Empty>
        </Card>
      </div>
    );
  }

  return (
    <div style={{ padding: 24, maxWidth: 1200, margin: "0 auto" }}>
      <Card style={{ marginBottom: 24 }}>
        <Title level={2}>领养资料设置</Title>
        <Paragraph type="secondary">
          请上传您的领养证明材料。完整的资料有助于提高领养申请通过率。
          支持上传图片（JPG、PNG、GIF）和PDF文件。
        </Paragraph>
      </Card>

      <Card title="材料列表" loading={loading}>
        <List
          dataSource={DOC_TYPE_OPTIONS}
          renderItem={(item) => {
            const existingDoc = getDocByType(item.value);
            return (
              <List.Item>
                <div style={{ width: "100%" }}>
                  <Space
                    style={{ width: "100%", justifyContent: "space-between" }}
                  >
                    <Space>
                      <FileTextOutlined />
                      <Text strong>{item.label}</Text>
                      {item.required && <Tag color="red">必填</Tag>}
                      {existingDoc && <Tag color="green">已上传</Tag>}
                    </Space>
                    <Space>
                      {existingDoc && (
                        <>
                          <Button
                            size="small"
                            icon={<EyeOutlined />}
                            onClick={() => handlePreview(existingDoc.url)}
                          >
                            查看
                          </Button>
                          <Button
                            size="small"
                            danger
                            icon={<DeleteOutlined />}
                            onClick={() => handleDelete(existingDoc.id)}
                          >
                            删除
                          </Button>
                        </>
                      )}
                      <Upload
                        showUploadList={false}
                        beforeUpload={(file) => {
                          handleUpload(file, item.value);
                          return false;
                        }}
                        accept="image/*,.pdf"
                      >
                        <Button
                          size="small"
                          type={existingDoc ? "default" : "primary"}
                          icon={<UploadOutlined />}
                          loading={uploading}
                        >
                          {existingDoc ? "重新上传" : "上传"}
                        </Button>
                      </Upload>
                    </Space>
                  </Space>
                  {existingDoc && (
                    <div style={{ marginTop: 8 }}>
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        上传时间：{existingDoc.uploadedAt || "-"}
                      </Text>
                    </div>
                  )}
                </div>
              </List.Item>
            );
          }}
        />
      </Card>

      <Modal
        title="预览材料"
        open={previewVisible}
        onCancel={() => setPreviewVisible(false)}
        footer={[
          <Button key="close" onClick={() => setPreviewVisible(false)}>
            关闭
          </Button>,
        ]}
        width={800}
      >
        {previewUrl && (
          <div style={{ textAlign: "center" }}>
            {previewUrl.toLowerCase().endsWith(".pdf") ? (
              <iframe
                src={previewUrl}
                style={{ width: "100%", height: "600px", border: "none" }}
                title="PDF预览"
              />
            ) : (
              <img
                src={previewUrl}
                alt="预览"
                style={{ maxWidth: "100%", maxHeight: "600px" }}
              />
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
