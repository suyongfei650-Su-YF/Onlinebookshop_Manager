USE bookshop_admin;

SET @col_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'admin_user'
    AND COLUMN_NAME = 'avatar_url'
);

SET @ddl = IF(
  @col_exists = 0,
  'ALTER TABLE admin_user ADD COLUMN avatar_url MEDIUMTEXT NULL COMMENT ''管理员头像(base64或URL)''',
  'ALTER TABLE admin_user MODIFY COLUMN avatar_url MEDIUMTEXT NULL COMMENT ''管理员头像(base64或URL)'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
