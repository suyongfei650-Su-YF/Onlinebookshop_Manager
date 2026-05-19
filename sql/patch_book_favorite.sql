-- 用户图书收藏表（已有库执行一次即可）
USE bookshop_admin;

CREATE TABLE IF NOT EXISTS book_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL COMMENT '读者 ID',
  book_id BIGINT NOT NULL COMMENT '图书 ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_favorite_customer_book (customer_id, book_id),
  CONSTRAINT fk_favorite_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE CASCADE,
  CONSTRAINT fk_favorite_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户图书收藏';
