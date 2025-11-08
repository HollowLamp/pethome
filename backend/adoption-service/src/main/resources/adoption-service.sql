-- 创建宠物领养申请表
CREATE TABLE adoption_app (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    pet_id BIGINT NOT NULL COMMENT '宠物ID',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    org_id BIGINT NOT NULL COMMENT '机构ID',
    status ENUM('PENDING', 'ORG_APPROVED', 'ORG_REJECTED', 'PLATFORM_APPROVED', 'PLATFORM_REJECTED', 'COMPLETED') DEFAULT 'PENDING' COMMENT "申请状态：PENDING待审核, ORG_APPROVED机构管理员审核通过, ORG_REJECTED机构管理员审核不通过, PLATFORM_APPROVED平台管理员审核通过, PLATFORM_REJECTED平台管理员审核不通过, COMPLETED已完成",
    reject_reason TEXT COMMENT '驳回原因，仅当状态为REJECTED时有效',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_pet_id (pet_id),
    KEY idx_applicant_id (applicant_id),
    KEY idx_org_id (org_id),
    KEY idx_status (status),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物领养申请记录表';

-- 创建领养材料上传记录表
CREATE TABLE adoption_doc (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '关联的申请ID',
    doc_type VARCHAR(32) NOT NULL COMMENT '材料类型，如：ID_CARD, INCOME_PROOF, PET_HISTORY 等',
    url VARCHAR(255) NOT NULL COMMENT '材料文件存储路径（如OSS链接）',
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',

    PRIMARY KEY (id),
    KEY idx_app_id (app_id),
    KEY idx_doc_type (doc_type),
    KEY idx_uploaded_at (uploaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领养申请相关材料上传记录表';

-- 创建面谈记录表
CREATE TABLE interview_record (
    id BIGINT AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '关联的申请ID',
    org_id BIGINT NOT NULL COMMENT '执行面谈的机构ID',
    start_at DATETIME COMMENT '面谈开始时间',
    end_at DATETIME COMMENT '面谈结束时间',
    status ENUM('REQUESTED', 'CONFIRMED', 'DONE') DEFAULT 'REQUESTED' COMMENT '面谈状态：REQUESTED(已请求), CONFIRMED(已确认), DONE(已完成)',
    note TEXT COMMENT '面谈备注或记录',

    PRIMARY KEY (id),
    KEY idx_app_id (app_id),
    KEY idx_org_id (org_id),
    KEY idx_start_at (start_at),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领养申请面谈记录表';