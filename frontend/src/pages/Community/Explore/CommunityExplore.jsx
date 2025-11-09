import React, { useState, useEffect, useMemo, useRef } from "react";
import {
  Card,
  Segmented,
  Select,
  Empty,
  Skeleton,
  App as AntdApp,
  Button,
  Space,
  Tag,
  Avatar,
  Carousel,
} from "antd";
import {
  FireOutlined,
  ClockCircleOutlined,
  LikeOutlined,
  MessageOutlined,
  PlusOutlined,
  FilterOutlined,
  RedoOutlined,
  StarFilled,
  LeftOutlined,
  RightOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router";
import api from "../../../api";
import useAuthStore from "../../../store/authStore";
import { processMediaUrls, processImageUrl } from "../../../utils/imageUtils";
import styles from "./CommunityExplore.module.css";

const TYPE_OPTIONS = [
  { label: "全部", value: undefined },
  { label: "养宠日常", value: "DAILY" },
  { label: "养宠攻略", value: "GUIDE" },
  { label: "宠物发布", value: "PET_PUBLISH" },
];

const SORT_OPTIONS = [
  { label: "推荐", value: "recommend" },
  { label: "最新", value: "latest" },
  { label: "最热", value: "popular" },
];

const PAGE_SIZE = 12;

export default function CommunityExplore() {
  const { message } = AntdApp.useApp();
  const navigate = useNavigate();
  const { isLoggedIn } = useAuthStore();

  const [posts, setPosts] = useState([]);
  const [recommendedPosts, setRecommendedPosts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [loadingRecommended, setLoadingRecommended] = useState(false);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [filters, setFilters] = useState({
    type: undefined,
    sort: "recommend",
  });

  const sentinelRef = useRef(null);
  const carouselRef = useRef(null);

  const filterSummary = useMemo(() => {
    const typeLabel =
      TYPE_OPTIONS.find((item) => item.value === filters.type)?.label || "全部";
    const sortLabel =
      SORT_OPTIONS.find((item) => item.value === filters.sort)?.label || "推荐";
    return `${typeLabel} / ${sortLabel}`;
  }, [filters]);

  // 获取推荐帖子
  const fetchRecommendedPosts = async () => {
    setLoadingRecommended(true);
    try {
      const res = await api.community.getPostList({
        page: 1,
        pageSize: 6,
        sort: "recommend",
      });
      if (res?.code === 200) {
        const list = res.data?.list || [];
        // 只显示被标记为推荐的帖子
        const recommended = list.filter((post) => post.recommend === true);
        setRecommendedPosts(recommended);
      }
    } catch (error) {
      console.error("获取推荐帖子失败:", error);
    } finally {
      setLoadingRecommended(false);
    }
  };

  const fetchPosts = async ({ pageNo, append = false }) => {
    if (loading || loadingMore) return;

    append ? setLoadingMore(true) : setLoading(true);
    setHasError(false);
    try {
      const res = await api.community.getPostList({
        page: pageNo,
        pageSize: PAGE_SIZE,
        type: filters.type,
        sort: filters.sort,
      });
      if (res?.code === 200) {
        const list = res.data?.list || [];
        setPosts((prev) => (append ? [...prev, ...list] : list));
        const total = res.data?.total ?? 0;
        setHasMore(pageNo * PAGE_SIZE < total);
        setPage(pageNo);
      } else {
        // 如果响应码不是 200，也停止加载更多
        setHasMore(false);
        setHasError(true);
      }
    } catch (error) {
      message.error(error?.message || "获取帖子列表失败");
      // 请求失败时停止加载更多，防止无限请求
      setHasMore(false);
      setHasError(true);
    } finally {
      append ? setLoadingMore(false) : setLoading(false);
    }
  };

  useEffect(() => {
    // 首次加载时获取推荐帖子
    fetchRecommendedPosts();
  }, []);

  useEffect(() => {
    setPage(1);
    setHasMore(true);
    setHasError(false);
    fetchPosts({ pageNo: 1, append: false });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters.type, filters.sort]);

  useEffect(() => {
    if (!sentinelRef.current) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (
          entries[0].isIntersecting &&
          hasMore &&
          !loading &&
          !loadingMore &&
          !hasError
        ) {
          fetchPosts({ pageNo: page + 1, append: true });
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
  }, [page, hasMore, loading, loadingMore, hasError, filters]);

  const getTypeColor = (type) => {
    const colorMap = {
      DAILY: "blue",
      GUIDE: "green",
      PET_PUBLISH: "orange",
    };
    return colorMap[type] || "default";
  };

  const getTypeLabel = (type) => {
    const option = TYPE_OPTIONS.find((item) => item.value === type);
    return option?.label || type;
  };

  const formatNumber = (num) => {
    if (num >= 10000) return `${(num / 10000).toFixed(1)}万`;
    return num;
  };

  // 渲染帖子卡片
  const renderPostCard = (post) => {
    const mediaUrls = processMediaUrls(post.mediaUrls);
    const coverImage = mediaUrls[0] || "/images/post-placeholder.png";

    return (
      <div
        key={post.id}
        className={styles.postCard}
        onClick={() => navigate(`/community/${post.id}`)}
      >
        <div className={styles.coverWrapper}>
          <img
            src={coverImage}
            alt={post.title}
            className={styles.cover}
            onError={(e) => {
              const originalUrl = e.target.src;
              if (
                !originalUrl.includes("/files/") &&
                !originalUrl.includes("placeholder")
              ) {
                e.target.src = `/files/${originalUrl.replace(
                  /^.*\/files\//,
                  ""
                )}`;
              } else if (!originalUrl.includes("placeholder")) {
                e.target.src = "/images/post-placeholder.png";
              }
            }}
          />
          {post.recommend && (
            <div className={styles.recommendBadge}>
              <StarFilled /> 推荐
            </div>
          )}
          <div className={styles.coverMask}>
            <span className={styles.coverAction}>查看详情</span>
          </div>
        </div>

        <div className={styles.cardBody}>
          <div className={styles.postHeader}>
            <Space size={8}>
              <Avatar
                src={processImageUrl(post.authorAvatarUrl)}
                size={32}
                style={{ backgroundColor: "#ff7f5d" }}
              >
                {post.authorName?.[0] || "U"}
              </Avatar>
              <div>
                <div className={styles.authorName}>
                  {post.authorName || "匿名用户"}
                </div>
                <div className={styles.postTime}>
                  {new Date(post.createdAt).toLocaleDateString()}
                </div>
              </div>
            </Space>
            <Tag color={getTypeColor(post.type)} className={styles.typeTag}>
              {getTypeLabel(post.type)}
            </Tag>
          </div>

          <h3 className={styles.postTitle}>{post.title}</h3>
          <p className={styles.postExcerpt}>
            {post.content?.substring(0, 80)}
            {post.content?.length > 80 ? "..." : ""}
          </p>

          <div className={styles.cardFooter}>
            <Space size={16}>
              <span className={styles.stat}>
                <LikeOutlined />
                {formatNumber(post.likeCount || 0)}
              </span>
              <span className={styles.stat}>
                <MessageOutlined />
                {formatNumber(post.commentCount || 0)}
              </span>
            </Space>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className={styles.page}>
      <div className={styles.hero}>
        <div className={styles.heroContent}>
          <h1>宠物社区</h1>
          <p>分享你的养宠故事，获取养宠攻略，发现更多爱宠伙伴</p>
          <Space size={12} className={styles.heroButtons}>
            {isLoggedIn ? (
              <Button
                type="primary"
                size="large"
                icon={<PlusOutlined />}
                onClick={() => navigate("/community/create")}
              >
                发布帖子
              </Button>
            ) : (
              <Button
                type="primary"
                size="large"
                onClick={() => {
                  window.dispatchEvent(new Event("OPEN_LOGIN_MODAL"));
                }}
              >
                登录发帖
              </Button>
            )}
            <Button
              size="large"
              onClick={() => window.scrollTo({ top: 280, behavior: "smooth" })}
            >
              浏览帖子
            </Button>
          </Space>
        </div>
        <div className={styles.heroTips}>
          <Space direction="vertical">
            <span>
              <FireOutlined /> 热门话题：养宠经验分享、宠物训练技巧
            </span>
            <span>社区每日更新 | 互动交流无限</span>
          </Space>
        </div>
      </div>

      {/* 推荐帖子轮播 */}
      {recommendedPosts.length > 0 && (
        <div className={styles.recommendSection}>
          <div className={styles.sectionHeader}>
            <FireOutlined className={styles.sectionIcon} />
            <h2>精选推荐</h2>
          </div>
          {loadingRecommended ? (
            <Card className={styles.carouselCard}>
              <Skeleton active paragraph={{ rows: 6 }} />
            </Card>
          ) : (
            <div className={styles.carouselWrapper}>
              <Carousel
                ref={carouselRef}
                autoplay
                autoplaySpeed={4000}
                className={styles.carousel}
                dots={true}
                arrows={false}
              >
                {recommendedPosts.map((post) => {
                  const mediaUrls = processMediaUrls(post.mediaUrls);
                  const coverImage =
                    mediaUrls[0] || "/images/post-placeholder.png";
                  return (
                    <div key={post.id}>
                      <div
                        className={styles.carouselItem}
                        onClick={(e) => {
                          // 如果点击的是导航区域，不触发跳转
                          if (
                            e.target.closest(`.${styles.carouselNavLeft}`) ||
                            e.target.closest(`.${styles.carouselNavRight}`)
                          ) {
                            return;
                          }
                          navigate(`/community/${post.id}`);
                        }}
                      >
                        <div className={styles.carouselImage}>
                          <img
                            src={coverImage}
                            alt={post.title}
                            onError={(e) => {
                              const originalUrl = e.target.src;
                              if (
                                !originalUrl.includes("/files/") &&
                                !originalUrl.includes("placeholder")
                              ) {
                                e.target.src = `/files/${originalUrl.replace(
                                  /^.*\/files\//,
                                  ""
                                )}`;
                              } else if (!originalUrl.includes("placeholder")) {
                                e.target.src = "/images/post-placeholder.png";
                              }
                            }}
                          />
                        </div>
                        <div className={styles.carouselContent}>
                          <Tag
                            color={getTypeColor(post.type)}
                            className={styles.carouselTag}
                          >
                            {getTypeLabel(post.type)}
                          </Tag>
                          <h3 className={styles.carouselTitle}>{post.title}</h3>
                          <p className={styles.carouselExcerpt}>
                            {post.content?.substring(0, 100)}
                            {post.content?.length > 100 ? "..." : ""}
                          </p>
                          <div className={styles.carouselFooter}>
                            <Space size={8}>
                              <Avatar
                                src={processImageUrl(post.authorAvatarUrl)}
                                size={24}
                                style={{ backgroundColor: "#ff7f5d" }}
                              >
                                {post.authorName?.[0] || "U"}
                              </Avatar>
                              <span className={styles.carouselAuthor}>
                                {post.authorName || "匿名用户"}
                              </span>
                            </Space>
                            <Space size={20}>
                              <span className={styles.carouselStat}>
                                <LikeOutlined />{" "}
                                {formatNumber(post.likeCount || 0)}
                              </span>
                              <span className={styles.carouselStat}>
                                <MessageOutlined />{" "}
                                {formatNumber(post.commentCount || 0)}
                              </span>
                            </Space>
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </Carousel>
              {/* 左侧点击区域 */}
              <div
                className={styles.carouselNavLeft}
                onClick={(e) => {
                  e.stopPropagation();
                  e.preventDefault();
                  // Ant Design 5.x Carousel ref 访问方式
                  const carousel = carouselRef.current;
                  if (carousel && carousel.prev) {
                    carousel.prev();
                  } else if (carousel && carousel.goTo) {
                    // 备用方案：获取当前索引并减1
                    const current = carousel.innerSlider?.currentSlide || 0;
                    carousel.goTo(Math.max(0, current - 1));
                  }
                }}
              >
                <LeftOutlined />
              </div>
              {/* 右侧点击区域 */}
              <div
                className={styles.carouselNavRight}
                onClick={(e) => {
                  e.stopPropagation();
                  e.preventDefault();
                  // Ant Design 5.x Carousel ref 访问方式
                  const carousel = carouselRef.current;
                  if (carousel && carousel.next) {
                    carousel.next();
                  } else if (carousel && carousel.goTo) {
                    // 备用方案：获取当前索引并加1
                    const current = carousel.innerSlider?.currentSlide || 0;
                    const total = recommendedPosts.length;
                    carousel.goTo(Math.min(total - 1, current + 1));
                  }
                }}
              >
                <RightOutlined />
              </div>
            </div>
          )}
        </div>
      )}

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
            onClick={() => setFilters({ type: undefined, sort: "recommend" })}
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
            value={filters.sort}
            onChange={(value) =>
              setFilters((prev) => ({ ...prev, sort: value }))
            }
            options={SORT_OPTIONS}
            style={{ width: 160 }}
          />
        </div>
      </Card>

      <div className={styles.postsGrid}>
        {loading && posts.length === 0
          ? Array.from({ length: PAGE_SIZE }).map((_, index) => (
              <Card key={index} className={styles.skeletonCard}>
                <Skeleton.Image
                  active
                  style={{ width: "100%", height: 220, borderRadius: 12 }}
                />
                <Skeleton
                  active
                  paragraph={{ rows: 3 }}
                  style={{ marginTop: 12 }}
                />
              </Card>
            ))
          : posts.map((post) => renderPostCard(post))}
      </div>

      {!loading && posts.length === 0 && (
        <Empty
          description="暂无帖子，快来发布第一条吧~"
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
        {hasError && posts.length > 0 && (
          <div className={styles.errorTip}>
            <span>加载失败</span>
            <Button
              type="link"
              onClick={() => {
                setHasError(false);
                setHasMore(true);
                fetchPosts({ pageNo: page + 1, append: true });
              }}
            >
              点击重试
            </Button>
          </div>
        )}
        {!hasMore && !hasError && posts.length > 0 && (
          <span className={styles.noMore}>没有更多啦~</span>
        )}
      </div>
    </div>
  );
}
