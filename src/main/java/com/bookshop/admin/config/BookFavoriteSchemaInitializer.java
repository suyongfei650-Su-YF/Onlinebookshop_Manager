package com.bookshop.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 启动时确保收藏表存在，避免未执行 SQL 补丁时收藏接口报错。
 */
@Component
@Order(100)
public class BookFavoriteSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BookFavoriteSchemaInitializer.class);

    private static final String DDL = "CREATE TABLE IF NOT EXISTS book_favorite ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
            + "customer_id BIGINT NOT NULL COMMENT '读者 ID', "
            + "book_id BIGINT NOT NULL COMMENT '图书 ID', "
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
            + "UNIQUE KEY uk_favorite_customer_book (customer_id, book_id), "
            + "CONSTRAINT fk_favorite_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE CASCADE, "
            + "CONSTRAINT fk_favorite_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户图书收藏'";

    private final DataSource dataSource;

    public BookFavoriteSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute(DDL);
            log.info("book_favorite 表已就绪");
        } catch (Exception e) {
            log.warn("book_favorite 表自动创建失败，请手动执行 sql/patch_book_favorite.sql: {}", e.getMessage());
        }
    }
}
