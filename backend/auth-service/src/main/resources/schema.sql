CREATE TABLE IF NOT EXISTS user_account (
                                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            username VARCHAR(64) NOT NULL,
                                            email VARCHAR(128),
                                            phone VARCHAR(32),
                                            password_hash VARCHAR(255) NOT NULL,
                                            status ENUM('ACTIVE','BANNED') DEFAULT 'ACTIVE' NOT NULL,
                                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS user_role (
                                         id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                         user_id BIGINT NOT NULL,
                                         role ENUM('USER','ORG_ADMIN','ORG_STAFF','AUDITOR','CS','ADMIN') NOT NULL,
                                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                         CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE,
                                         INDEX idx_user_role_user_id (user_id),
                                         INDEX idx_user_role_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
