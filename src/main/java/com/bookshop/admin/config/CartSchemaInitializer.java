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

@Component
@Order(101)
public class CartSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CartSchemaInitializer.class);

    private static final String DDL = "CREATE TABLE IF NOT EXISTS cart_item ("
            + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
            + "customer_id BIGINT NOT NULL COMMENT '读者 ID', "
            + "book_id BIGINT NOT NULL COMMENT '图书 ID', "
            + "quantity INT NOT NULL DEFAULT 1 COMMENT '数量', "
            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
            + "updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP, "
            + "UNIQUE KEY uk_cart_customer_book (customer_id, book_id), "
            + "CONSTRAINT fk_cart_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE CASCADE, "
            + "CONSTRAINT fk_cart_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户购物车'";

    private final DataSource dataSource;

    public CartSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute(DDL);
            log.info("cart_item 表已就绪");
        } catch (Exception e) {
            log.warn("cart_item 表自动创建失败，请执行 sql/patch_cart_item.sql: {}", e.getMessage());
        }
    }
}
