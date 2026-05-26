-- 读者留言板（用户端）
CREATE TABLE IF NOT EXISTS guestbook_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL COMMENT '读者 ID',
  content VARCHAR(500) NOT NULL COMMENT '留言内容',
  status VARCHAR(16) NOT NULL DEFAULT 'VISIBLE' COMMENT 'VISIBLE/HIDDEN',
  admin_reply VARCHAR(500) NULL COMMENT '管理员回复',
  admin_reply_at TIMESTAMP NULL COMMENT '回复时间',
  admin_replier_id BIGINT NULL COMMENT '回复管理员 ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_guestbook_created (created_at DESC),
  CONSTRAINT fk_guestbook_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='读者留言板';
