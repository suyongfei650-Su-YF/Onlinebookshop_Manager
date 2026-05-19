package com.bookshop.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.entity.NewsletterSubscription;
import com.bookshop.admin.mapper.NewsletterSubscriptionMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/portal")
public class PortalNewsletterController {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int EMAIL_MAX_LEN = 128;

    private final NewsletterSubscriptionMapper newsletterSubscriptionMapper;

    public PortalNewsletterController(NewsletterSubscriptionMapper newsletterSubscriptionMapper) {
        this.newsletterSubscriptionMapper = newsletterSubscriptionMapper;
    }

    @PostMapping("/newsletter/subscribe")
    public ResponseEntity<ApiResult<Map<String, Object>>> subscribe(@RequestBody(required = false) Map<String, Object> body) {
        try {
            if (body == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请求体不能为空"));
            }
            String email = String.valueOf(body.getOrDefault("email", "")).trim().toLowerCase();
            if (email.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请输入电子邮箱"));
            }
            if (email.length() > EMAIL_MAX_LEN) {
                return ResponseEntity.badRequest().body(ApiResult.fail("邮箱地址过长"));
            }
            if (!EMAIL.matcher(email).matches()) {
                return ResponseEntity.badRequest().body(ApiResult.fail("邮箱格式不正确"));
            }

            Long exists = newsletterSubscriptionMapper.selectCount(
                    new LambdaQueryWrapper<NewsletterSubscription>().eq(NewsletterSubscription::getEmail, email));
            String message;
            if (exists != null && exists > 0) {
                message = "您已订阅成功，感谢关注！";
            } else {
                NewsletterSubscription row = new NewsletterSubscription();
                row.setEmail(email);
                row.setSource("portal_home");
                row.setCreatedAt(LocalDateTime.now());
                newsletterSubscriptionMapper.insert(row);
                message = "订阅成功，感谢关注！";
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", message);
            return ResponseEntity.ok(ApiResult.ok(data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }
}
