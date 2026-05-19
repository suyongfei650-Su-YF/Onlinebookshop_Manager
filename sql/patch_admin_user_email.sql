-- 已有库升级：为 admin_user 增加 email（执行一次即可）
USE bookshop_admin;
ALTER TABLE admin_user ADD COLUMN email VARCHAR(128) NULL COMMENT '找回密码时须与账号匹配' AFTER display_name;
UPDATE admin_user SET email = 'admin@archivist.demo' WHERE username = 'admin' AND (email IS NULL OR email = '');
