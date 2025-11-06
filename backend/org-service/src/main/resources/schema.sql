-- org 表：机构信息
CREATE TABLE IF NOT EXISTS org (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  name         VARCHAR(128)   NOT NULL,
  license_url  VARCHAR(255)   NOT NULL,
  address      VARCHAR(255)   NOT NULL,
  contact_name VARCHAR(64)    NOT NULL,
  contact_phone VARCHAR(32)   NOT NULL,
  status       VARCHAR(32)    NOT NULL,
  created_by   BIGINT         NOT NULL,
  created_at   DATETIME       NOT NULL,
  updated_at   DATETIME       NOT NULL
);

-- org_member 表：机构成员
CREATE TABLE IF NOT EXISTS org_member (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id     BIGINT        NOT NULL,
  user_id    BIGINT        NOT NULL,
  role       VARCHAR(32)   NOT NULL,
  created_at DATETIME      NOT NULL,
  UNIQUE KEY uk_org_user (org_id, user_id)
);


