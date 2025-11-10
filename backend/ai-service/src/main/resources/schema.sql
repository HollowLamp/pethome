-- AI 任务表
-- 用于记录所有 AI 分析任务
CREATE TABLE IF NOT EXISTS ai_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type ENUM('STATE_EXTRACT', 'CONTENT_MOD', 'SUMMARY') NOT NULL COMMENT '任务类型：STATE_EXTRACT-状态提取, CONTENT_MOD-内容审核, SUMMARY-内容总结',
    source_id BIGINT NOT NULL COMMENT '来源ID（如帖子ID）',
    source_type ENUM('POST', 'PET') NOT NULL DEFAULT 'POST' COMMENT '来源类型：POST-帖子, PET-宠物',
    status ENUM('PENDING', 'DONE', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING-待处理, DONE-已完成, FAILED-失败',
    result_json JSON COMMENT '分析结果（JSON格式）',
    confidence DECIMAL(3,2) COMMENT '置信度（0.00-1.00）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_source (source_type, source_id),
    INDEX idx_status (status),
    INDEX idx_type (type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI任务表';

