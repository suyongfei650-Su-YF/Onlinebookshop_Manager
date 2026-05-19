package com.bookshop.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bookshop.admin.mapper")
public class BookshopAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookshopAdminApplication.class, args);
    }
}
