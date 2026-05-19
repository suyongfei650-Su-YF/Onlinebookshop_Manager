-- 首页期刊邮箱订阅表（已有库执行一次即可）
USE bookshop_admin;

CREATE TABLE IF NOT EXISTS newsletter_subscription (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(128) NOT NULL COMMENT '订阅邮箱',
  source VARCHAR(32) NOT NULL DEFAULT 'portal_home' COMMENT '来源页面',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_newsletter_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页期刊邮箱订阅';
