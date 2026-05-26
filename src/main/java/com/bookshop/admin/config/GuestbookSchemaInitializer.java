package com.bookshop.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 启动时确保读者留言表存在，并补齐管理员回复相关字段。
 */
@Component
@Order(101)
public class GuestbookSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GuestbookSchemaInitializer.class);

    private static final String DDL = "CREATE TABLE IF NOT EXISTS guestbook_message ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
            + "customer_id BIGINT NOT NULL COMMENT '读者 ID', "
            + "content VARCHAR(500) NOT NULL COMMENT '留言内容', "
            + "status VARCHAR(16) NOT NULL DEFAULT 'VISIBLE' COMMENT 'VISIBLE/HIDDEN', "
            + "admin_reply VARCHAR(500) NULL COMMENT '管理员回复', "
            + "admin_reply_at TIMESTAMP NULL COMMENT '回复时间', "
            + "admin_replier_id BIGINT NULL COMMENT '回复管理员 ID', "
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
            + "INDEX idx_guestbook_created (created_at DESC), "
            + "CONSTRAINT fk_guestbook_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE CASCADE"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='读者留言板'";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public GuestbookSchemaInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute(DDL);
            log.info("guestbook_message 表已就绪");
        } catch (Exception e) {
            log.warn("guestbook_message 表自动创建失败，请手动执行 sql/patch_guestbook_message.sql: {}", e.getMessage());
        }
        ensureColumn("admin_reply", "ALTER TABLE guestbook_message ADD COLUMN admin_reply VARCHAR(500) NULL COMMENT '管理员回复'");
        ensureColumn("admin_reply_at", "ALTER TABLE guestbook_message ADD COLUMN admin_reply_at TIMESTAMP NULL COMMENT '回复时间'");
        ensureColumn("admin_replier_id", "ALTER TABLE guestbook_message ADD COLUMN admin_replier_id BIGINT NULL COMMENT '回复管理员 ID'");
    }

    private void ensureColumn(String columnName, String alterSql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() "
                            + "AND TABLE_NAME = 'guestbook_message' "
                            + "AND COLUMN_NAME = ?",
                    Integer.class,
                    columnName);
            if (count != null && count == 0) {
                jdbcTemplate.execute(alterSql);
                log.info("guestbook_message 已添加列 {}", columnName);
            }
        } catch (Exception e) {
            log.warn("guestbook_message 列 {} 补丁跳过: {}", columnName, e.getMessage());
        }
    }
}
