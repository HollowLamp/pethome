-- 面谈时段表
CREATE TABLE IF NOT EXISTS schedule_slot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id BIGINT NOT NULL COMMENT '机构ID',
    start_at DATETIME NOT NULL COMMENT '开始时间',
    end_at DATETIME NOT NULL COMMENT '结束时间',
    is_open BOOLEAN DEFAULT TRUE COMMENT '是否可预约',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_org_id (org_id),
    INDEX idx_start_at (start_at),
    INDEX idx_is_open (is_open)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='面谈时段表';

-- 面谈预约表
CREATE TABLE IF NOT EXISTS interview_booking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    app_id BIGINT NOT NULL COMMENT '申请ID',
    slot_id BIGINT NOT NULL COMMENT '面谈时段ID',
    status ENUM('REQUESTED','CONFIRMED','CANCELED','DONE') DEFAULT 'REQUESTED' COMMENT '状态：REQUESTED待确认, CONFIRMED已确认, CANCELED已取消, DONE已完成',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_interview_booking_slot FOREIGN KEY (slot_id) REFERENCES schedule_slot(id) ON DELETE RESTRICT,
    INDEX idx_app_id (app_id),
    INDEX idx_slot_id (slot_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='面谈预约表';

