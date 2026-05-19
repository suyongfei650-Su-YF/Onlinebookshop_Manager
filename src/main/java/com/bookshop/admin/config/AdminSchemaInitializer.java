package com.bookshop.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class AdminSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminSchemaInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    public AdminSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() " +
                            "AND TABLE_NAME = 'admin_user' " +
                            "AND COLUMN_NAME = 'avatar_url'",
                    Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE admin_user ADD COLUMN avatar_url MEDIUMTEXT NULL");
            } else if (count != null && count > 0) {
                jdbcTemplate.execute("ALTER TABLE admin_user MODIFY COLUMN avatar_url MEDIUMTEXT NULL");
            }
        } catch (Exception e) {
            log.warn("skip admin schema patch: {}", e.getMessage());
        }
    }
}
