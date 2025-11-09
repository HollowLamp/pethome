import React, { useState, useEffect } from "react";
import {
  Card,
  Form,
  Input,
  Select,
  Button,
  Upload,
  Space,
  App as AntdApp,
  Image,
  Spin,
} from "antd";
import {
  PlusOutlined,
  ArrowLeftOutlined,
  DeleteOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router";
import api from "../../../api";
import useAuthStore from "../../../store/authStore";
import styles from "./CreatePost.module.css";

const { TextArea } = Input;

const TYPE_OPTIONS = [
  { label: "养宠日常", value: "DAILY" },
  { label: "养宠攻略", value: "GUIDE" },
  { label: "宠物发布", value: "PET_PUBLISH" },
];

export default function CreatePost() {
  const { message } = AntdApp.useApp();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const { user } = useAuthStore();
  const userId = user?.id || user?.userId;

  const [uploading, setUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [fileList, setFileList] = useState([]);
  const [adoptedPets, setAdoptedPets] = useState([]);
  const [availablePets, setAvailablePets] = useState([]);
  const [loadingPets, setLoadingPets] = useState(false);
  const [postType, setPostType] = useState("DAILY");
  const [orgId, setOrgId] = useState(null);
  const [orgOptions, setOrgOptions] = useState([]);
  const [orgLoading, setOrgLoading] = useState(false);

  const handleUpload = async (options) => {
    const { file, onSuccess, onError } = options;

    if (fileList.length >= 9) {
      message.warning("最多只能上传9个文件");
      onError();
      return;
    }

    setUploading(true);
    try {
      const res = await api.community.uploadFile(file);
      if (res?.code === 200) {
        // 处理URL：如果是完整URL直接使用，否则添加/files/前缀
        let displayUrl = res.data.url;
        if (displayUrl && !displayUrl.startsWith("http")) {
          // 如果URL不是以http开头，需要添加/files/前缀
          if (!displayUrl.startsWith("/")) {
            displayUrl = `/files/${displayUrl}`;
          } else if (!displayUrl.startsWith("/files/")) {
            displayUrl = `/files${displayUrl}`;
          }
        }

        const newFile = {
          uid: file.uid,
          name: file.name,
          status: "done",
          url: displayUrl, // 用于显示的URL
          originalUrl: res.data.url, // 保存原始URL用于提交
          relativePath: res.data.relativePath || res.data.url, // 保存相对路径用于存储
        };
        setFileList((prev) => [...prev, newFile]);
        onSuccess(res.data);
        message.success("上传成功");
      }
    } catch (error) {
      message.error(error?.message || "上传失败");
      onError(error);
    } finally {
      setUploading(false);
    }
  };

  const handleRemove = (file) => {
    setFileList((prev) => prev.filter((item) => item.uid !== file.uid));
  };

  // 加载已领养宠物列表
  const loadAdoptedPets = async () => {
    setLoadingPets(true);
    try {
      const res = await api.adoption.getAdoptedPets();
      if (res?.code === 200) {
        // 从AdoptionApp中提取petId，然后获取宠物详情
        const petIds = res.data
          .map((app) => app.petId)
          .filter((id) => id != null);
        const petDetails = await Promise.all(
          petIds.map((petId) => api.pets.getPetDetail(petId))
        );
        const pets = petDetails
          .filter((res) => res?.code === 200)
          .map((res) => res.data);
        setAdoptedPets(pets);
      }
    } catch (error) {
      console.error("加载已领养宠物失败:", error);
    } finally {
      setLoadingPets(false);
    }
  };

  // 获取用户所属机构列表
  const fetchUserMemberships = async () => {
    if (!userId) {
      setOrgId(null);
      setOrgOptions([]);
      return;
    }
    setOrgLoading(true);
    try {
      const res = await api.org.getUserMemberships(userId);
      if (res?.code === 200) {
        const list = Array.isArray(res.data)
          ? res.data
          : res.data?.list || res.data?.memberships || [];

        // 映射机构列表
        const mapped = list
          .map((item) => {
            const org = item.organizationId ? item : item.org || item;
            const id = org.orgId || org.organizationId || org.id;
            if (!id) return null;
            return {
              label: org.name || `机构 ${id}`,
              value: id,
            };
          })
          .filter(Boolean);

        setOrgOptions(mapped);

        // 如果有机构，默认选择第一个
        if (mapped.length > 0) {
          setOrgId((prev) => prev ?? mapped[0].value);
        } else {
          setOrgId(null);
        }
      } else {
        setOrgOptions([]);
        setOrgId(null);
      }
    } catch (error) {
      console.error("获取用户机构信息失败:", error);
      setOrgOptions([]);
      setOrgId(null);
    } finally {
      setOrgLoading(false);
    }
  };

  // 加载可领养宠物列表（用于宠物发布）- 只加载当前用户所属机构的宠物
  const loadAvailablePets = async () => {
    if (!orgId) {
      setAvailablePets([]);
      return;
    }

    setLoadingPets(true);
    try {
      // 根据机构ID筛选可领养宠物
      const res = await api.pets.fetchPets({
        status: "AVAILABLE",
        orgId: orgId,
        page: 1,
        pageSize: 100,
      });
      if (res?.code === 200) {
        setAvailablePets(res.data?.list || []);
        if (res.data?.list?.length === 0) {
          message.info("您的机构暂无可领养的宠物");
        }
      }
    } catch (error) {
      console.error("加载可领养宠物失败:", error);
      message.error("加载宠物列表失败");
    } finally {
      setLoadingPets(false);
    }
  };

  // 获取用户机构（参考admin页面的实现）
  useEffect(() => {
    fetchUserMemberships();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  // 当机构ID或帖子类型变化时，加载对应的宠物列表
  useEffect(() => {
    if (!userId) return;

    if (postType === "DAILY") {
      loadAdoptedPets();
    } else if (postType === "PET_PUBLISH") {
      if (orgId) {
        loadAvailablePets();
      } else {
        setAvailablePets([]);
      }
    } else {
      setAdoptedPets([]);
      setAvailablePets([]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId, postType, orgId]);

  const handleTypeChange = (value) => {
    setPostType(value);
    form.setFieldsValue({ bindPetId: undefined }); // 清空宠物选择
  };

  const handleSubmit = async (values) => {
    setSubmitting(true);
    try {
      // 使用relativePath或originalUrl存储，优先使用relativePath
      const mediaUrls = fileList.map((file) => {
        // 优先使用relativePath（相对路径）
        if (file.relativePath) {
          return file.relativePath;
        }
        // 如果没有relativePath，使用originalUrl
        if (file.originalUrl) {
          // 如果是完整URL，提取相对路径；如果是相对路径，直接使用
          if (file.originalUrl.startsWith("http")) {
            // 从完整URL中提取路径部分（如果需要）
            return file.originalUrl;
          }
          return file.originalUrl;
        }
        // 最后使用url（去掉/files/前缀）
        return file.url?.replace(/^\/files\//, "") || file.url;
      });

      const payload = {
        ...values,
        mediaUrls: JSON.stringify(mediaUrls),
        // 只有DAILY和PET_PUBLISH需要bindPetId
        bindPetId:
          values.type === "GUIDE" ? undefined : values.bindPetId || undefined,
      };

      const res = await api.community.createPost(payload);
      if (res?.code === 200) {
        message.success("发布成功");
        navigate(`/community/${res.data.id}`);
      }
    } catch (error) {
      message.error(error?.message || "发布失败");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate("/community")}
          style={{ marginBottom: 16 }}
        >
          返回列表
        </Button>

        <Card className={styles.formCard} title="发布帖子">
          <Form
            form={form}
            layout="vertical"
            onFinish={handleSubmit}
            initialValues={{ type: "DAILY" }}
          >
            <Form.Item
              label="帖子类型"
              name="type"
              rules={[{ required: true, message: "请选择帖子类型" }]}
            >
              <Select
                options={TYPE_OPTIONS}
                placeholder="选择帖子类型"
                size="large"
                onChange={handleTypeChange}
              />
            </Form.Item>

            {/* 养宠日常：必须选择已领养宠物 */}
            {postType === "DAILY" && (
              <Form.Item
                label="关联宠物（必选）"
                name="bindPetId"
                rules={[
                  {
                    required: true,
                    message: "养宠日常必须关联一只已领养的宠物",
                  },
                ]}
              >
                <Select
                  placeholder="选择你已领养的宠物"
                  size="large"
                  loading={loadingPets}
                  showSearch
                  optionFilterProp="label"
                  options={adoptedPets.map((pet) => ({
                    label: `${pet.name || "未命名"} (${pet.type || ""})`,
                    value: pet.id,
                  }))}
                />
              </Form.Item>
            )}

            {/* 宠物发布：可选择机构，然后选择该机构下的可领养宠物 */}
            {postType === "PET_PUBLISH" && (
              <>
                {/* 如果用户有多个机构，显示机构选择器 */}
                {orgOptions.length > 1 && (
                  <Form.Item label="选择机构" tooltip="选择要发布宠物的机构">
                    <Select
                      placeholder="选择机构"
                      size="large"
                      loading={orgLoading}
                      value={orgId}
                      onChange={(value) => {
                        setOrgId(value);
                        form.setFieldsValue({ bindPetId: undefined }); // 清空宠物选择
                      }}
                      options={orgOptions}
                    />
                  </Form.Item>
                )}

                {/* 如果只有一个机构，显示机构名称 */}
                {orgOptions.length === 1 && (
                  <Form.Item label="机构">
                    <Input
                      value={orgOptions[0]?.label || "未知机构"}
                      size="large"
                      disabled
                    />
                  </Form.Item>
                )}

                <Form.Item
                  label="关联宠物（可选）"
                  name="bindPetId"
                  tooltip={
                    orgId
                      ? "选择您所属机构下的可领养宠物，如果不选择则仅发布文字信息"
                      : "请先加入机构后才能发布宠物信息"
                  }
                >
                  <Select
                    placeholder={
                      orgId
                        ? "选择您机构下的可领养宠物（可选）"
                        : "您不属于任何机构，无法选择宠物"
                    }
                    size="large"
                    loading={loadingPets}
                    showSearch
                    optionFilterProp="label"
                    allowClear
                    disabled={!orgId}
                    options={availablePets.map((pet) => ({
                      label: `${pet.name || "未命名"} (${pet.type || ""})`,
                      value: pet.id,
                    }))}
                  />
                </Form.Item>
              </>
            )}

            <Form.Item
              label="标题"
              name="title"
              rules={[
                { required: true, message: "请输入标题" },
                { max: 100, message: "标题最多100个字符" },
              ]}
            >
              <Input
                placeholder="给你的帖子起个标题吧"
                size="large"
                showCount
                maxLength={100}
              />
            </Form.Item>

            <Form.Item
              label="内容"
              name="content"
              rules={[
                { required: true, message: "请输入内容" },
                { min: 10, message: "内容至少10个字符" },
                { max: 5000, message: "内容最多5000个字符" },
              ]}
            >
              <TextArea
                rows={12}
                placeholder="分享你的养宠故事或经验..."
                showCount
                maxLength={5000}
              />
            </Form.Item>

            <Form.Item label="图片/视频（可选，最多9个）">
              <div className={styles.uploadArea}>
                {fileList.map((file) => {
                  // 确保URL格式正确
                  const imageUrl = file.url || file.originalUrl;
                  const isVideo = file.name
                    ?.toLowerCase()
                    .match(/\.(mp4|avi|mov|wmv|flv|webm)$/);

                  return (
                    <div key={file.uid} className={styles.uploadItem}>
                      {isVideo ? (
                        <video
                          src={imageUrl}
                          style={{
                            width: "100%",
                            height: "100%",
                            objectFit: "cover",
                          }}
                          controls={false}
                          muted
                        />
                      ) : (
                        <img
                          src={imageUrl}
                          alt={file.name || "上传的图片"}
                          onError={(e) => {
                            // 如果加载失败，尝试使用原始URL
                            if (
                              file.originalUrl &&
                              e.target.src !== file.originalUrl
                            ) {
                              e.target.src = file.originalUrl;
                            } else {
                              // 如果还是失败，显示占位符
                              e.target.src = "/images/placeholder.png";
                              e.target.onerror = null; // 防止无限循环
                            }
                          }}
                          style={{
                            width: "100%",
                            height: "100%",
                            objectFit: "cover",
                            display: "block",
                          }}
                        />
                      )}
                      <Button
                        type="text"
                        danger
                        icon={<DeleteOutlined />}
                        className={styles.deleteBtn}
                        onClick={() => handleRemove(file)}
                      />
                    </div>
                  );
                })}
                {fileList.length < 9 && (
                  <Upload
                    customRequest={handleUpload}
                    showUploadList={false}
                    accept="image/*,video/*"
                    disabled={uploading}
                  >
                    <div className={styles.uploadButton}>
                      <PlusOutlined style={{ fontSize: 24 }} />
                      <div style={{ marginTop: 8 }}>
                        {uploading ? "上传中..." : "上传"}
                      </div>
                    </div>
                  </Upload>
                )}
              </div>
              <div className={styles.uploadTip}>
                支持 JPG、PNG、GIF、MP4 格式，单个文件不超过 10MB
              </div>
            </Form.Item>

            <Form.Item>
              <Space size={12}>
                <Button
                  type="primary"
                  htmlType="submit"
                  size="large"
                  loading={submitting}
                  disabled={uploading}
                >
                  发布
                </Button>
                <Button
                  size="large"
                  onClick={() => navigate("/community")}
                  disabled={submitting || uploading}
                >
                  取消
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Card>

        <Card className={styles.tipsCard} title="💡 发帖小贴士">
          <ul className={styles.tipsList}>
            <li>
              <strong>养宠日常：</strong>
              分享你和宠物的日常生活，可能会触发宠物状态的自动更新哦
            </li>
            <li>
              <strong>养宠攻略：</strong>
              分享你的养宠经验和技巧，AI 会自动生成摘要帮助更多人
            </li>
            <li>
              <strong>宠物发布：</strong>
              发布待领养的宠物信息，让更多人看到
            </li>
            <li>请文明发言，尊重他人，违规内容将被删除或封禁账号</li>
            <li>上传清晰的图片和视频能获得更多关注</li>
          </ul>
        </Card>
      </div>
    </div>
  );
}
