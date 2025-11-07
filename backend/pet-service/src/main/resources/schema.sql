-- 宠物表
CREATE TABLE IF NOT EXISTS pet (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    type ENUM('DOG','CAT','RABBIT','BIRD','HAMSTER','GUINEA_PIG','FERRET','TURTLE','FISH','OTHER') NOT NULL,
    breed VARCHAR(64),
    gender ENUM('MALE','FEMALE'),
    age INT,
    color VARCHAR(32),
    size ENUM('SMALL','MEDIUM','LARGE'),
    status ENUM('AVAILABLE','RESERVED','ADOPTED','ARCHIVED') DEFAULT 'AVAILABLE' NOT NULL,
    cover_url VARCHAR(255),
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_org_id (org_id),
    INDEX idx_status (status),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 宠物健康记录表
CREATE TABLE IF NOT EXISTS pet_health (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pet_id BIGINT NOT NULL,
    weight DECIMAL(5,2),
    vaccine JSON,
    note TEXT,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pet_health_pet FOREIGN KEY (pet_id) REFERENCES pet(id) ON DELETE CASCADE,
    INDEX idx_pet_id (pet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 宠物反馈表
CREATE TABLE IF NOT EXISTS pet_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pet_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT,
    media_urls JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pet_feedback_pet FOREIGN KEY (pet_id) REFERENCES pet(id) ON DELETE CASCADE,
    INDEX idx_pet_id (pet_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 愿望单表
CREATE TABLE IF NOT EXISTS wishlist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    pet_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_pet UNIQUE KEY (user_id, pet_id),
    CONSTRAINT fk_wishlist_pet FOREIGN KEY (pet_id) REFERENCES pet(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_pet_id (pet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

