-- 通知任务表
CREATE TABLE IF NOT EXISTS notify_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '接收者',
    channel ENUM('SYSTEM','EMAIL','SMS','PUSH') NOT NULL DEFAULT 'SYSTEM' COMMENT '渠道',
    template_code VARCHAR(64) COMMENT '模板',
    payload JSON COMMENT '参数',
    status ENUM('PENDING','SENT','FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 收件箱消息表
CREATE TABLE IF NOT EXISTS inbox_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    to_user_id BIGINT NOT NULL COMMENT '接收者',
    title VARCHAR(128) NOT NULL COMMENT '标题',
    body TEXT COMMENT '内容',
    is_read BOOLEAN NOT NULL DEFAULT FALSE COMMENT '已读标记',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    INDEX idx_to_user_id (to_user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 私信表
CREATE TABLE IF NOT EXISTS direct_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_user_id BIGINT NOT NULL COMMENT '发送者',
    to_user_id BIGINT NOT NULL COMMENT '接收者',
    content TEXT NOT NULL COMMENT '内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    INDEX idx_from_user_id (from_user_id),
    INDEX idx_to_user_id (to_user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

