CREATE TABLE user_account (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              username VARCHAR(64) NOT NULL,
                              email VARCHAR(128),
                              phone VARCHAR(32),
                              password_hash VARCHAR(255) NOT NULL,
                              status ENUM('ACTIVE','BANNED') DEFAULT 'ACTIVE',
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_role (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           user_id BIGINT NOT NULL,
                           role ENUM('USER','ORG_ADMIN','ORG_STAFF','AUDITOR','CS','ADMIN'),
                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);