-- 馆藏书屋 · 管理员端数据库（MySQL 8+）
-- 执行: mysql -u root -p < sql/bookshop_admin.sql

CREATE DATABASE IF NOT EXISTS bookshop_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bookshop_admin;

-- 管理员
CREATE TABLE IF NOT EXISTS admin_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL COMMENT '演示环境明文，生产请改为加盐哈希',
  display_name VARCHAR(64),
  avatar_url MEDIUMTEXT NULL COMMENT '管理员头像(base64或URL)',
  email VARCHAR(128) NULL COMMENT '找回密码时须与账号匹配',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 图书分类
CREATE TABLE IF NOT EXISTS category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NULL,
  name VARCHAR(128) NOT NULL,
  name_en VARCHAR(128),
  code VARCHAR(32),
  sort_weight INT NOT NULL DEFAULT 0,
  visible TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id)
) ENGINE=InnoDB;

-- 图书
CREATE TABLE IF NOT EXISTS book (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NULL,
  title VARCHAR(256) NOT NULL,
  author VARCHAR(128),
  isbn VARCHAR(32),
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  stock INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ON_SHELF' COMMENT 'ON_SHELF / OFF_SHELF',
  cover_url TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_book_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE=InnoDB;

-- 首页期刊邮箱订阅
CREATE TABLE IF NOT EXISTS newsletter_subscription (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(128) NOT NULL COMMENT '订阅邮箱',
  source VARCHAR(32) NOT NULL DEFAULT 'portal_home' COMMENT '来源页面',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_newsletter_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页期刊邮箱订阅';

-- 用户图书收藏
CREATE TABLE IF NOT EXISTS book_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL COMMENT '读者 ID',
  book_id BIGINT NOT NULL COMMENT '图书 ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_favorite_customer_book (customer_id, book_id),
  CONSTRAINT fk_favorite_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE CASCADE,
  CONSTRAINT fk_favorite_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户图书收藏';

CREATE TABLE IF NOT EXISTS cart_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_id BIGINT NOT NULL COMMENT '读者 ID',
  book_id BIGINT NOT NULL COMMENT '图书 ID',
  quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_cart_customer_book (customer_id, book_id),
  CONSTRAINT fk_cart_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户购物车';

-- 读者（用户管理）
CREATE TABLE IF NOT EXISTS customer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nickname VARCHAR(64) NOT NULL COMMENT '用户网名（前端昵称）',
  username VARCHAR(64) NULL UNIQUE COMMENT '登录账号（用户名），可为空；建议注册时填写',
  email VARCHAR(128) NULL UNIQUE COMMENT '邮箱（可用于登录/找回）',
  phone VARCHAR(32) NULL UNIQUE COMMENT '手机号（可用于登录）',
  password VARCHAR(128) NOT NULL DEFAULT '123456' COMMENT '演示环境明文，生产请改为加盐哈希',
  avatar_url MEDIUMTEXT NULL COMMENT '用户头像(base64或URL)',
  role_tag VARCHAR(64) COMMENT '如: 高级读者',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / DISABLED',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 兼容旧库：customer 表存在但缺字段/索引时，做一次“补丁式”升级
-- 注意：不要用 ADD COLUMN IF NOT EXISTS（部分 MySQL 版本不支持），这里用 information_schema + 动态 SQL
SET @db_name := DATABASE();

-- username
SET @col := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customer' AND COLUMN_NAME = 'username'
);
SET @sql := IF(@col = 0,
  "ALTER TABLE customer ADD COLUMN username VARCHAR(64) NULL COMMENT '登录账号（用户名），可为空；建议注册时填写'",
  "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- email
SET @col := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customer' AND COLUMN_NAME = 'email'
);
SET @sql := IF(@col = 0,
  "ALTER TABLE customer ADD COLUMN email VARCHAR(128) NULL COMMENT '邮箱（可用于登录/找回）'",
  "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- phone（旧表一般已有，这里只补不存在的情况）
SET @col := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customer' AND COLUMN_NAME = 'phone'
);
SET @sql := IF(@col = 0,
  "ALTER TABLE customer ADD COLUMN phone VARCHAR(32) NULL COMMENT '手机号（可用于登录）'",
  "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- password
SET @col := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customer' AND COLUMN_NAME = 'password'
);
SET @sql := IF(@col = 0,
  "ALTER TABLE customer ADD COLUMN password VARCHAR(128) NOT NULL DEFAULT '123456' COMMENT '演示环境明文，生产请改为加盐哈希'",
  "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- avatar_url
SET @col := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customer' AND COLUMN_NAME = 'avatar_url'
);
SET @sql := IF(@col = 0,
  "ALTER TABLE customer ADD COLUMN avatar_url MEDIUMTEXT NULL COMMENT '用户头像(base64或URL)'",
  "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- updated_at
SET @col := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customer' AND COLUMN_NAME = 'updated_at'
);
SET @sql := IF(@col = 0,
  "ALTER TABLE customer ADD COLUMN updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP",
  "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 补唯一索引（若不存在）
SET @idx := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customer' AND INDEX_NAME = 'uk_customer_username'
);
SET @sql := IF(@idx = 0, "CREATE UNIQUE INDEX uk_customer_username ON customer(username)", "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customer' AND INDEX_NAME = 'uk_customer_email'
);
SET @sql := IF(@idx = 0, "CREATE UNIQUE INDEX uk_customer_email ON customer(email)", "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'customer' AND INDEX_NAME = 'uk_customer_phone'
);
SET @sql := IF(@idx = 0, "CREATE UNIQUE INDEX uk_customer_phone ON customer(phone)", "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 订单
CREATE TABLE IF NOT EXISTS shop_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  customer_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(32) NOT NULL COMMENT 'PENDING_PAY / PENDING_SHIP / SHIPPED / DONE',
  receiver_name VARCHAR(64) NULL COMMENT '收货人',
  receiver_phone VARCHAR(32) NULL COMMENT '收货电话',
  shipping_address VARCHAR(512) NULL COMMENT '收货地址',
  payment_method VARCHAR(32) NULL COMMENT '支付方式 WECHAT/ALIPAY/CARD',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  book_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES shop_order (id),
  CONSTRAINT fk_item_book FOREIGN KEY (book_id) REFERENCES book (id)
) ENGINE=InnoDB;

-- 初始数据
INSERT INTO admin_user (username, password, display_name, email) VALUES
('admin', 'admin123', '系统管理员', 'admin@archivist.demo')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), email = COALESCE(VALUES(email), email);

INSERT INTO category (id, parent_id, name, name_en, code, sort_weight, visible) VALUES
(1, NULL, '哲学与思想', 'Philosophy', 'ARCHIVE_A', 1, 1),
(2, NULL, '文学与艺术', 'Literature', 'ARCHIVE_B', 2, 1),
(3, 1, '西方古典哲学', 'Western Classics', NULL, 10, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO book (id, category_id, title, author, isbn, price, stock, status, cover_url) VALUES
(1, 1, '时间简史', '史蒂芬·霍金', '978-7544291170', 68.00, 124, 'ON_SHELF', 'https://lh3.googleusercontent.com/aida-public/AB6AXuBJi2USsZgSlw2RMfCtRwGRrN9zZX-ws2cGheOrnOkjHtpff3fCCbPVj9MZ_rAxD9flox7Bu21w5UOWnsv-Zr4a9f1pugEwmjGzX5QaF2g_gSxp5GCXn8gd17epHWEs6caEKdQEVj6mQ9ZpSgSlqczLnHbHP6XAYq3cYXTHvELq30EFyL_OXx4jdzF4jr8YJLLOCWmm4p-LRNCvO_Z7TV2lRIkiJavkUf_q_qiDKP-9wVk5qy_emRgZrYhWucIxsnsuv-RSgkPeAQ'),
(2, 2, '艺术的力量', '西蒙·沙玛', '978-7550212459', 128.00, 8, 'ON_SHELF', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAv9RxuboCe5qgreoA0BsGQxJsRn8jDtI4GmNpuqmKpU5PUlaA2PgJhJiE-eNqhuiXa04E3q5RWynAxX9s54NyCWSyRa_drtiYUQ_-EVoJdrwuAz0KLXXs1roqCXDhkmOYudbLJSu9K97QTrCWStUVOm965hiz1X5q1PEieKXhHW6dMXLrVEKS57fF9yy07ntsDxJzBl0QCiIEyQ8b4tK5elZFyuFqRwoTE2RMKJbuVgZjlZnhAS247CXqt8lmPDm023xuP6fYM7A'),
(3, 1, '理想国 (绝版)', '柏拉图', '978-7100017565', 45.00, 0, 'OFF_SHELF', 'https://lh3.googleusercontent.com/aida-public/AB6AXuBVvF18gM7FF2EXFBwyrp7DuUKNtC5YTRwyIyLuEt-IQhjt-9C9V9mAvU0540FVVqI1m-IW8s-Teh1OxlbyyiePiJdi4Hu8HS5HQOmczV-yMcgSocWDoE3SEBwSRtqDYsjFY99RNsXBgUIcswp0pBeEchH5YBdsnk11ChY5cQLexwwyCEEoy7FiMffcBvbzrJmc4HnasApbeA1C06DlnVBTCBtKUUx27FXZm254wxGF5s-9TS2PNQv17FJwHLACayIIT5RaqI6law')
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  cover_url = VALUES(cover_url);

-- 扩展图书数据（基于公开图书信息补充）
INSERT INTO book (id, category_id, title, author, isbn, price, stock, status, cover_url) VALUES
(10, 3, '三体', '刘慈欣', '9787536692930', 45.00, 120, 'ON_SHELF', 'https://covers.openlibrary.org/b/isbn/9787536692930-L.jpg'),
(11, 3, '三体II：黑暗森林', '刘慈欣', '9787536693968', 48.00, 95, 'ON_SHELF', 'https://covers.openlibrary.org/b/isbn/9787536693968-L.jpg'),
(12, 3, '三体III：死神永生', '刘慈欣', '9787229100629', 52.00, 88, 'ON_SHELF', 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="240" height="360"><rect width="100%" height="100%" fill="%23111827"/><text x="50%" y="44%" fill="%23F9FAFB" font-size="22" text-anchor="middle">SanTi III</text><text x="50%" y="56%" fill="%23D1D5DB" font-size="14" text-anchor="middle">Liu Cixin</text></svg>'),
(13, 2, '活着', '余华', '9787530221532', 45.00, 150, 'ON_SHELF', 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="240" height="360"><rect width="100%" height="100%" fill="%231F2937"/><text x="50%" y="44%" fill="%23F3F4F6" font-size="22" text-anchor="middle">Huozhe</text><text x="50%" y="56%" fill="%23D1D5DB" font-size="14" text-anchor="middle">Yu Hua</text></svg>'),
(14, 2, '解忧杂货店', '东野圭吾', '9787544270878', 39.50, 132, 'ON_SHELF', 'https://covers.openlibrary.org/b/isbn/9787544270878-L.jpg'),
(15, 1, '人类简史', '尤瓦尔·赫拉利', '9787508660752', 68.00, 110, 'ON_SHELF', 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="240" height="360"><rect width="100%" height="100%" fill="%230F172A"/><text x="50%" y="44%" fill="%23E2E8F0" font-size="22" text-anchor="middle">Sapiens</text><text x="50%" y="56%" fill="%23CBD5E1" font-size="14" text-anchor="middle">Harari</text></svg>'),
(16, 2, '白夜行', '东野圭吾', '9787544291163', 59.00, 86, 'ON_SHELF', 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="240" height="360"><rect width="100%" height="100%" fill="%23374151"/><text x="50%" y="44%" fill="%23F9FAFB" font-size="22" text-anchor="middle">Byakuyako</text><text x="50%" y="56%" fill="%23D1D5DB" font-size="14" text-anchor="middle">Keigo Higashino</text></svg>'),
(17, 2, '嫌疑人X的献身', '东野圭吾', '9787544241694', 35.00, 140, 'ON_SHELF', 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="240" height="360"><rect width="100%" height="100%" fill="%23334155"/><text x="50%" y="44%" fill="%23F8FAFC" font-size="18" text-anchor="middle">The Devotion</text><text x="50%" y="56%" fill="%23CBD5E1" font-size="14" text-anchor="middle">Keigo Higashino</text></svg>')
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  author = VALUES(author),
  isbn = VALUES(isbn),
  price = VALUES(price),
  stock = VALUES(stock),
  status = VALUES(status),
  cover_url = VALUES(cover_url);

INSERT INTO customer (id, nickname, username, email, phone, password, role_tag, status) VALUES
(1, '张三的书阁', 'zhangsan', 'zhangsan@example.com', '13800009021', '123456', '高级读者', 'ACTIVE'),
(2, '墨香雅筑', 'moxiang', 'moxiang@example.com', '13900001115', '123456', '认证学者', 'ACTIVE'),
(3, '陈雨沫', 'chenyumo', 'chenyumo@example.com', '13100000912', '123456', '普通读者', 'DISABLED'),
(4, '林间文存', 'linjian', 'linjian@example.com', '15900002284', '123456', '资深会员', 'ACTIVE')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  phone = VALUES(phone),
  role_tag = VALUES(role_tag),
  status = VALUES(status);

INSERT INTO shop_order (id, order_no, customer_id, total_amount, status, created_at) VALUES
(1, '#ORD-20231024-01', 1, 298.00, 'PENDING_SHIP', '2023-10-24 14:30:00'),
(2, '#ORD-20231024-02', 2, 156.50, 'SHIPPED', '2023-10-24 11:15:00'),
(3, '#ORD-20231023-88', 4, 42.00, 'PENDING_PAY', '2023-10-23 20:45:00'),
(4, '#ORD-20231023-45', 2, 810.00, 'DONE', '2023-10-23 16:20:00')
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT INTO order_item (order_id, book_id, quantity, unit_price) VALUES
(1, 1, 2, 68.00),
(2, 2, 1, 128.00),
(3, 10, 1, 42.00),
(4, 2, 5, 128.00),
(4, 1, 5, 68.00);
