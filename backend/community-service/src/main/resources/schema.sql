-- 帖子表
CREATE TABLE IF NOT EXISTS post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    type ENUM('PET_PUBLISH','DAILY','GUIDE') NOT NULL,
    title VARCHAR(128) NOT NULL,
    content TEXT,
    media_urls JSON,
    bind_pet_id BIGINT NULL,
    ai_summary TEXT,
    ai_flagged BOOLEAN DEFAULT FALSE,
    status ENUM('PUBLISHED','FLAGGED','REMOVED') DEFAULT 'PUBLISHED',
    recommend BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_author_id (author_id),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_bind_pet_id (bind_pet_id),
    INDEX idx_recommend (recommend),
    INDEX idx_ai_flagged (ai_flagged),
    INDEX idx_created_at (created_at),
    CONSTRAINT fk_post_pet FOREIGN KEY (bind_pet_id) REFERENCES pet(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 评论表
CREATE TABLE IF NOT EXISTS comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    status ENUM('VISIBLE','REMOVED') DEFAULT 'VISIBLE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_post_id (post_id),
    INDEX idx_author_id (author_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 点赞表
CREATE TABLE IF NOT EXISTS reaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NULL,
    comment_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    type ENUM('LIKE') NOT NULL DEFAULT 'LIKE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    -- 确保只有一个目标
    CONSTRAINT chk_reaction_target CHECK (
        (post_id IS NOT NULL AND comment_id IS NULL) OR 
        (post_id IS NULL AND comment_id IS NOT NULL)
    ),
    -- 防止重复点赞
    CONSTRAINT uk_reaction_user_post UNIQUE KEY (user_id, post_id, type),
    CONSTRAINT uk_reaction_user_comment UNIQUE KEY (user_id, comment_id, type),
    
    INDEX idx_post_id (post_id),
    INDEX idx_comment_id (comment_id),
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_reaction_post FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    CONSTRAINT fk_reaction_comment FOREIGN KEY (comment_id) REFERENCES comment(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 举报表
CREATE TABLE IF NOT EXISTS report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NULL,
    comment_id BIGINT NULL,
    reporter_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status ENUM('PENDING','REVIEWED') DEFAULT 'PENDING',
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_report_target CHECK (
        (post_id IS NOT NULL AND comment_id IS NULL) OR 
        (post_id IS NULL AND comment_id IS NOT NULL)
    ),
    -- 防止重复举报
    CONSTRAINT uk_report_user_post UNIQUE KEY (reporter_id, post_id),
    CONSTRAINT uk_report_user_comment UNIQUE KEY (reporter_id, comment_id),
    
    INDEX idx_post_id (post_id),
    INDEX idx_comment_id (comment_id),
    INDEX idx_reporter_id (reporter_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    CONSTRAINT fk_report_post FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    CONSTRAINT fk_report_comment FOREIGN KEY (comment_id) REFERENCES comment(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

