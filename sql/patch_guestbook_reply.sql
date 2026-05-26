-- 留言板管理员回复字段（已有 guestbook_message 表时执行；重复执行若列已存在会报错可忽略）
ALTER TABLE guestbook_message ADD COLUMN admin_reply VARCHAR(500) NULL COMMENT '管理员回复' AFTER status;
ALTER TABLE guestbook_message ADD COLUMN admin_reply_at TIMESTAMP NULL COMMENT '回复时间' AFTER admin_reply;
ALTER TABLE guestbook_message ADD COLUMN admin_replier_id BIGINT NULL COMMENT '回复管理员 ID' AFTER admin_reply_at;
