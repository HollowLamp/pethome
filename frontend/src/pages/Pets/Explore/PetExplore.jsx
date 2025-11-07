import React, { useState, useEffect, useMemo, useRef } from "react";
import {
  Card,
  Segmented,
  Select,
  Empty,
  Skeleton,
  message,
  Button,
  Space,
} from "antd";
import {
  AppstoreOutlined,
  FireOutlined,
  FilterOutlined,
  RedoOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router";
import api from "../../../api";
import useAuthStore from "../../../store/authStore";
import PetCard from "../components/PetCard";
import styles from "./PetExplore.module.css";

const TYPE_OPTIONS = [
  { label: "全部", value: undefined },
  { label: "狗狗", value: "DOG" },
  { label: "猫咪", value: "CAT" },
  { label: "兔子", value: "RABBIT" },
  { label: "小鸟", value: "BIRD" },
  { label: "仓鼠", value: "HAMSTER" },
  { label: "豚鼠", value: "GUINEA_PIG" },
  { label: "乌龟", value: "TURTLE" },
  { label: "鱼", value: "FISH" },
  { label: "其他", value: "OTHER" },
];

const STATUS_OPTIONS = [
  { label: "全部", value: undefined },
  { label: "可领养", value: "AVAILABLE" },
  { label: "已预约", value: "RESERVED" },
];

const PAGE_SIZE = 12;

export default function PetExplore() {
  const navigate = useNavigate();
  const { isLoggedIn } = useAuthStore();

  const [pets, setPets] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [filters, setFilters] = useState({
    type: undefined,
    status: undefined,
    sort: "recommend",
  });

  const sentinelRef = useRef(null);

  const filterSummary = useMemo(() => {
    const typeLabel =
      TYPE_OPTIONS.find((item) => item.value === filters.type)?.label || "全部";
    const statusLabel =
      STATUS_OPTIONS.find((item) => item.value === filters.status)?.label ||
      "全部";
    return `${typeLabel} / ${statusLabel}`;
  }, [filters]);

  const fetchPets = async ({ pageNo, append = false }) => {
    if (loading || loadingMore) return;

    append ? setLoadingMore(true) : setLoading(true);
    try {
      const res = await api.pets.fetchPets({
        page: pageNo,
        pageSize: PAGE_SIZE,
        type: filters.type,
        status: filters.status,
      });
      if (res?.code === 200) {
        const list = res.data?.list || [];
        setPets((prev) => (append ? [...prev, ...list] : list));
        const total = res.data?.total ?? 0;
        setHasMore(pageNo * PAGE_SIZE < total);
        setPage(pageNo);
      }
    } catch (error) {
      message.error(error?.message || "获取宠物列表失败");
    } finally {
      append ? setLoadingMore(false) : setLoading(false);
    }
  };

  useEffect(() => {
    setPage(1);
    fetchPets({ pageNo: 1, append: false });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters.type, filters.status]);

  useEffect(() => {
    if (!sentinelRef.current) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMore && !loading && !loadingMore) {
          fetchPets({ pageNo: page + 1, append: true });
        }
      },
      {
        root: null,
        rootMargin: "0px",
        threshold: 0.1,
      }
    );

    observer.observe(sentinelRef.current);
    return () => observer.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, hasMore, loading, loadingMore, filters]);

  return (
    <div className={styles.page}>
      <div className={styles.hero}>
        <div className={styles.heroContent}>
          <h1>发现你的心动伙伴</h1>
          <p>
            来自全国认证机构的真实宠物档案，登录即可查看更多健康信息与领养流程。
          </p>
          <Space size={12} className={styles.heroButtons}>
            <Button
              type="primary"
              size="large"
              icon={<AppstoreOutlined />}
              onClick={() => window.scrollTo({ top: 280, behavior: "smooth" })}
            >
              立即浏览
            </Button>
            {!isLoggedIn && (
              <Button size="large" onClick={() => navigate("/login")}>
                登录解锁更多
              </Button>
            )}
          </Space>
        </div>
        <div className={styles.heroTips}>
          <Space direction="vertical">
            <span>
              <FireOutlined /> 热门类型：小型犬、人气英短、乖巧橘猫
            </span>
            <span>平台每日更新 | 支持远程视频家访</span>
          </Space>
        </div>
      </div>

      <Card className={styles.filterCard}>
        <div className={styles.filterRow}>
          <Space size={16} align="center">
            <FilterOutlined style={{ color: "#1677ff" }} />
            <span className={styles.filterLabel}>筛选条件</span>
            <span className={styles.filterSummary}>{filterSummary}</span>
          </Space>
          <Button
            type="link"
            icon={<RedoOutlined />}
            onClick={() =>
              setFilters((prev) => ({
                ...prev,
                type: undefined,
                status: undefined,
              }))
            }
          >
            重置
          </Button>
        </div>
        <div className={styles.filterSelectors}>
          <Segmented
            options={TYPE_OPTIONS}
            value={filters.type}
            onChange={(value) =>
              setFilters((prev) => ({ ...prev, type: value }))
            }
          />
          <Select
            value={filters.status}
            onChange={(value) =>
              setFilters((prev) => ({ ...prev, status: value }))
            }
            options={STATUS_OPTIONS}
            style={{ width: 160 }}
          />
        </div>
      </Card>

      <div className={styles.grid}>
        {loading && pets.length === 0
          ? Array.from({ length: PAGE_SIZE }).map((_, index) => (
              <Card key={index} className={styles.skeletonCard}>
                <Skeleton.Image
                  active
                  style={{ width: "100%", height: 220, borderRadius: 12 }}
                />
                <Skeleton
                  active
                  paragraph={{ rows: 2 }}
                  style={{ marginTop: 12 }}
                />
              </Card>
            ))
          : pets.map((pet) => (
              <PetCard
                key={pet.id}
                pet={pet}
                onClick={() => navigate(`/pets/${pet.id}`)}
              />
            ))}
      </div>

      {!loading && pets.length === 0 && (
        <Empty
          description="暂无符合条件的宠物，试试调整筛选~"
          style={{ marginTop: 80 }}
        />
      )}

      <div ref={sentinelRef} className={styles.sentinel}>
        {loadingMore && (
          <div className={styles.loadingMore}>
            <Skeleton.Avatar active size={48} shape="circle" />
            <Skeleton
              active
              title={false}
              paragraph={{ rows: 1, width: 120 }}
              style={{ marginLeft: 16 }}
            />
          </div>
        )}
        {!hasMore && pets.length > 0 && (
          <span className={styles.noMore}>没有更多啦~</span>
        )}
      </div>
    </div>
  );
}
