-- 为图书表增加简介字段（若尚未添加）
USE bookshop_admin;

SET @col := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'book' AND COLUMN_NAME = 'description'
);
SET @sql := IF(@col = 0,
  "ALTER TABLE book ADD COLUMN description TEXT NULL COMMENT '图书简介' AFTER cover_url",
  "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
