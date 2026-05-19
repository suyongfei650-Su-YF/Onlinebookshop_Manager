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
@Order(102)
public class OrderShippingSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderShippingSchemaInitializer.class);

    private static final String[] ALTERS = {
            "ALTER TABLE shop_order ADD COLUMN receiver_name VARCHAR(64) NULL COMMENT '收货人'",
            "ALTER TABLE shop_order ADD COLUMN receiver_phone VARCHAR(32) NULL COMMENT '收货电话'",
            "ALTER TABLE shop_order ADD COLUMN shipping_address VARCHAR(512) NULL COMMENT '收货地址'",
            "ALTER TABLE shop_order ADD COLUMN payment_method VARCHAR(32) NULL COMMENT '支付方式'"
    };

    private final DataSource dataSource;

    public OrderShippingSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            for (String sql : ALTERS) {
                try {
                    st.execute(sql);
                } catch (Exception ignored) {
                    /* 列已存在 */
                }
            }
            log.info("shop_order 收货字段已就绪");
        } catch (Exception e) {
            log.warn("shop_order 收货字段升级失败，可执行 sql/patch_order_shipping.sql: {}", e.getMessage());
        }
    }
}
