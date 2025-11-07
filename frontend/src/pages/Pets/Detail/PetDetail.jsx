import React, { useEffect, useMemo, useState } from "react";
import {
  Card,
  Typography,
  Space,
  Tag,
  Button,
  Descriptions,
  Divider,
  message,
  Tabs,
  List,
  Skeleton,
  Empty,
  Upload,
  Modal,
  Input,
} from "antd";
import {
  ShareAltOutlined,
  HeartOutlined,
  HeartFilled,
  CheckCircleOutlined,
  SmileOutlined,
} from "@ant-design/icons";
import { useParams } from "react-router";
import api from "../../../api";
import useAuthStore from "../../../store/authStore";
import styles from "./PetDetail.module.css";

const { Title, Text, Paragraph } = Typography;

const TYPE_LABELS = {
  DOG: "狗狗",
  CAT: "猫咪",
  RABBIT: "兔子",
  BIRD: "小鸟",
  OTHER: "其他",
};

const GENDER_LABELS = {
  MALE: "男孩",
  FEMALE: "女孩",
};

const SIZE_LABELS = {
  SMALL: "小型",
  MEDIUM: "中型",
  LARGE: "大型",
};

const STATUS_COLORS = {
  AVAILABLE: "green",
  RESERVED: "gold",
  ADOPTED: "blue",
  ARCHIVED: "default",
};

export default function PetDetail() {
  const { petId } = useParams();

  const { isLoggedIn } = useAuthStore();

  const [pet, setPet] = useState(null);
  const [loading, setLoading] = useState(false);
  const [health, setHealth] = useState(null);
  const [healthLoading, setHealthLoading] = useState(false);
  const [feedbacks, setFeedbacks] = useState([]);
  const [feedbackLoading, setFeedbackLoading] = useState(false);
  const [feedbackLocked, setFeedbackLocked] = useState(false);
  const [wishlistLoading, setWishlistLoading] = useState(false);
  const [inWishlist, setInWishlist] = useState(false);

  const coverUrl = useMemo(() => {
    if (!pet?.coverUrl) return "/images/pet-placeholder.png";
    return pet.coverUrl.startsWith("http")
      ? pet.coverUrl
      : `/files/${pet.coverUrl}`;
  }, [pet]);

  const fetchDetail = async () => {
    setLoading(true);
    try {
      const res = await api.pets.getPetDetail(petId);
      if (res?.code === 200) {
        setPet(res.data);
        setInWishlist(Boolean(res.data?.inWishlist));
      }
    } catch (error) {
      message.error(error?.message || "获取宠物详情失败");
    } finally {
      setLoading(false);
    }
  };

  const fetchHealth = async () => {
    if (!isLoggedIn) {
      setHealth(null);
      return;
    }
    setHealthLoading(true);
    try {
      const res = await api.pets.getPetHealth(petId);
      if (res?.code === 200) {
        setHealth(res.data);
      } else {
        setHealth(null);
      }
    } catch {
      setHealth(null);
    } finally {
      setHealthLoading(false);
    }
  };

  const fetchFeedbacksByType = async (type) => {
    if (!isLoggedIn) {
      setFeedbackLocked(true);
      setFeedbacks([]);
      return;
    }
    if (!type) return;
    setFeedbackLocked(false);
    setFeedbackLoading(true);
    try {
      const res = await api.pets.getPetFeedbacksByType(type);
      if (res?.code === 200) {
        setFeedbacks(res.data || []);
      } else {
        setFeedbacks([]);
      }
    } catch (e) {
      message.error(e?.message || "获取反馈失败");
    } finally {
      setFeedbackLoading(false);
    }
  };

  useEffect(() => {
    fetchDetail();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [petId]);

  useEffect(() => {
    fetchHealth();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoggedIn, petId]);

  // 当获取到当前宠物类型或登录状态变化，拉取同类型的反馈
  useEffect(() => {
    if (pet?.type) {
      fetchFeedbacksByType(pet.type);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pet?.type, isLoggedIn]);

  const handleWishlist = async () => {
    if (!isLoggedIn) {
      message.info("登录后才能收藏宠物哦~");
      window.dispatchEvent(new Event("OPEN_LOGIN_MODAL"));
      return;
    }
    setWishlistLoading(true);
    try {
      if (inWishlist) {
        await api.pets.removeFromWishlist(petId);
        message.success("已从愿望单移除");
        setInWishlist(false);
      } else {
        await api.pets.addToWishlist(petId);
        message.success("已加入愿望单");
        setInWishlist(true);
      }
    } catch (error) {
      message.error(error?.message || "操作失败，请稍后重试");
    } finally {
      setWishlistLoading(false);
    }
  };

  const handleApply = () => {
    if (!isLoggedIn) {
      message.info("请先登录后再提交领养申请");
      window.dispatchEvent(new Event("OPEN_LOGIN_MODAL"));
      return;
    }
    message.info("领养申请流程将上线，敬请期待~");
  };

  // 反馈上传已移除，保留展示逻辑

  const renderHealthInfo = () => {
    if (!isLoggedIn) {
      return (
        <div className={styles.healthMask}>
          <Text>登录后可查看完整健康与疫苗记录</Text>
          <Button
            type="primary"
            size="small"
            onClick={() => window.dispatchEvent(new Event("OPEN_LOGIN_MODAL"))}
          >
            去登录
          </Button>
        </div>
      );
    }

    if (healthLoading) {
      return <Skeleton active paragraph={{ rows: 2 }} />;
    }

    if (!health) {
      return (
        <Empty
          description="暂无健康记录"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      );
    }

    const vaccines = (() => {
      if (!health.vaccine) return [];
      try {
        const arr = JSON.parse(health.vaccine);
        return Array.isArray(arr) ? arr : [];
      } catch {
        return [];
      }
    })();

    return (
      <Descriptions column={1} size="small" bordered>
        <Descriptions.Item label="最近体重">
          {health.weight ? `${health.weight} kg` : "-"}
        </Descriptions.Item>
        <Descriptions.Item label="已接种疫苗">
          <Space wrap>
            {vaccines.length > 0
              ? vaccines.map((item) => <Tag key={item}>{item}</Tag>)
              : "无"}
          </Space>
        </Descriptions.Item>
        <Descriptions.Item label="备注">
          {health.note || "未填写"}
        </Descriptions.Item>
        <Descriptions.Item label="最后更新时间">
          {health.updatedAt || "-"}
        </Descriptions.Item>
      </Descriptions>
    );
  };

  return (
    <div className={styles.page}>
      <Card className={styles.banner} loading={loading}>
        {!loading && (
          <div className={styles.bannerInner}>
            <div className={styles.coverBox}>
              <img src={coverUrl} alt={pet?.name} />
            </div>
            <div className={styles.infoBox}>
              <Title level={2}>{pet?.name || "未命名宠物"}</Title>
              <Space size={8} wrap>
                <Tag color="gold">
                  {TYPE_LABELS[pet?.type] || pet?.type || "未知"}
                </Tag>
                {pet?.gender && (
                  <Tag>{GENDER_LABELS[pet.gender] || pet.gender}</Tag>
                )}
                {pet?.size && <Tag>{SIZE_LABELS[pet.size] || pet.size}</Tag>}
                {pet?.status && (
                  <Tag color={STATUS_COLORS[pet.status] || "blue"}>
                    {pet.status === "AVAILABLE" ? "可领养" : pet.status}
                  </Tag>
                )}
              </Space>
              <Paragraph className={styles.description}>
                {pet?.description || "暂无简介"}
              </Paragraph>

              <Space size={16}>
                <Button
                  type="primary"
                  icon={<CheckCircleOutlined />}
                  size="large"
                  onClick={handleApply}
                >
                  申请领养
                </Button>
                <Button
                  size="large"
                  loading={wishlistLoading}
                  icon={inWishlist ? <HeartFilled /> : <HeartOutlined />}
                  onClick={handleWishlist}
                >
                  {inWishlist ? "已收藏" : "加入愿望单"}
                </Button>
              </Space>
            </div>
          </div>
        )}
      </Card>

      <div className={styles.contentGrid}>
        <Card title="基础信息" bordered={false} className={styles.infoCard}>
          {loading ? (
            <Skeleton active paragraph={{ rows: 4 }} />
          ) : (
            <Descriptions column={2} size="small">
              <Descriptions.Item label="宠物编号">{pet?.id}</Descriptions.Item>
              <Descriptions.Item label="所属机构">
                {pet?.orgName || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="品种">
                {pet?.breed || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="颜色">
                {pet?.color || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="年龄">
                {pet?.age != null ? `${pet.age} 岁` : "-"}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {pet?.createdAt || "-"}
              </Descriptions.Item>
            </Descriptions>
          )}
        </Card>

        <Card
          title="健康 & 疫苗记录"
          bordered={false}
          className={styles.infoCard}
        >
          {renderHealthInfo()}
        </Card>
      </div>

      <Tabs
        className={styles.tabs}
        items={[
          {
            key: "feedback",
            label: "领养反馈",
            children: (
              <Card bordered={false} title="同类型宠物的领养反馈">
                {feedbackLocked ? (
                  <div className={styles.blurSection}>
                    <div className={styles.blurMask}>
                      登录后可查看同类型宠物的领养反馈
                    </div>
                  </div>
                ) : feedbackLoading ? (
                  <Skeleton active paragraph={{ rows: 3 }} />
                ) : feedbacks.length > 0 ? (
                  <List
                    dataSource={feedbacks}
                    itemLayout="vertical"
                    renderItem={(item) => {
                      let mediaUrls = [];
                      if (item.mediaUrls) {
                        try {
                          const parsed = JSON.parse(item.mediaUrls);
                          if (Array.isArray(parsed)) {
                            mediaUrls = parsed;
                          }
                        } catch {
                          mediaUrls = [];
                        }
                      }
                      const createdAt = item.createdAt
                        ? new Date(item.createdAt).toLocaleString()
                        : "--";
                      return (
                        <List.Item>
                          <Space
                            direction="vertical"
                            size={8}
                            style={{ width: "100%" }}
                          >
                            <Space align="center" size={12}>
                              <SmileOutlined style={{ color: "#faad14" }} />
                              <Text strong>ID: {item.userId}</Text>
                              <Text type="secondary">{createdAt}</Text>
                            </Space>
                            <Paragraph style={{ marginBottom: 8 }}>
                              {item.content || "这位用户没有留下文字评论"}
                            </Paragraph>
                            {mediaUrls.length > 0 && (
                              <div className={styles.mediaGrid}>
                                {mediaUrls.map((url, index) => (
                                  <a
                                    key={url}
                                    href={url}
                                    target="_blank"
                                    rel="noreferrer"
                                  >
                                    <div className={styles.mediaThumb}>
                                      <img
                                        src={url}
                                        alt={`反馈图片-${index + 1}`}
                                      />
                                    </div>
                                  </a>
                                ))}
                              </div>
                            )}
                          </Space>
                        </List.Item>
                      );
                    }}
                  />
                ) : (
                  <Empty description="暂无相关类型的反馈" />
                )}
              </Card>
            ),
          },
        ]}
      />
    </div>
  );
}
