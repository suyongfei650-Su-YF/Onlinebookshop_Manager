package com.bookshop.admin.config;

import com.bookshop.admin.service.BookCoverStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final BookCoverStorageService bookCoverStorageService;

    public WebMvcConfig(AdminAuthInterceptor adminAuthInterceptor, BookCoverStorageService bookCoverStorageService) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.bookCoverStorageService = bookCoverStorageService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns(
                        "/api/admin/login",
                        "/api/admin/logout",
                        "/api/admin/session",
                        "/api/admin/captcha",
                        "/api/admin/register",
                        "/api/admin/forgot-password");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 封面放在 src/main/webapp/book-covers，避免打进 WEB-INF/classes 导致 WAR 展开复制失败
        String fileLocation = bookCoverStorageService.getCoverDirectory().toUri().toString();
        if (!fileLocation.endsWith("/")) {
            fileLocation = fileLocation + "/";
        }
        registry.addResourceHandler("/book-covers/**")
                .addResourceLocations("/book-covers/", fileLocation);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
