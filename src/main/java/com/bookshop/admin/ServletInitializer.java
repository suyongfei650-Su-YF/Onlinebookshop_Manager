package com.bookshop.admin;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/** 外置 Tomcat 部署 WAR 时使用 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        // 外置 Tomcat 必须用 prod：context-path 为 /，由 Tomcat Application context 提供 /Onlinebookshop_Manager
        return builder.sources(BookshopAdminApplication.class).profiles("prod");
    }
}
