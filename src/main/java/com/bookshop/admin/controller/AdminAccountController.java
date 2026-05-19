package com.bookshop.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.entity.AdminUser;
import com.bookshop.admin.mapper.AdminUserMapper;
import com.bookshop.admin.util.CaptchaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminAccountController {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountController.class);

    private final AdminUserMapper adminUserMapper;
    private final CaptchaValidator captchaValidator;

    @Value("${app.admin.allow-register:true}")
    private boolean allowRegister;

    public AdminAccountController(AdminUserMapper adminUserMapper, CaptchaValidator captchaValidator) {
        this.adminUserMapper = adminUserMapper;
        this.captchaValidator = captchaValidator;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResult<Map<String, Object>>> register(
            @RequestBody(required = false) Map<String, Object> body,
            HttpSession session) {
        try {
            if (!allowRegister) {
                return ResponseEntity.status(403).body(ApiResult.fail("管理员注册未开放，请联系系统管理员"));
            }
            if (body == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请求体不能为空"));
            }
            String captcha = String.valueOf(body.getOrDefault("captcha", ""));
            if (!captchaValidator.validateAndConsume(session, captcha)) {
                return ResponseEntity.badRequest().body(ApiResult.fail("验证码错误或已过期，请刷新后重试"));
            }
            String username = String.valueOf(body.getOrDefault("username", "")).trim();
            String password = String.valueOf(body.getOrDefault("password", ""));
            String displayName = String.valueOf(body.getOrDefault("displayName", "")).trim();
            String email = String.valueOf(body.getOrDefault("email", "")).trim();
            if (username.isEmpty() || password.isEmpty() || displayName.isEmpty() || email.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请填写账号、密码、显示名称与邮箱"));
            }
            if (username.length() < 3 || username.length() > 64) {
                return ResponseEntity.badRequest().body(ApiResult.fail("用户名长度为 3～64 字符"));
            }
            if (password.length() < 6) {
                return ResponseEntity.badRequest().body(ApiResult.fail("密码至少 6 位"));
            }
            Long exists = adminUserMapper.selectCount(Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getUsername, username));
            if (exists != null && exists > 0) {
                return ResponseEntity.badRequest().body(ApiResult.fail("用户名已被占用"));
            }
            AdminUser u = new AdminUser();
            u.setUsername(username);
            u.setPassword(password);
            u.setDisplayName(displayName);
            u.setEmail(email);
            adminUserMapper.insert(u);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", u.getId());
            data.put("username", u.getUsername());
            return ResponseEntity.ok(ApiResult.ok(data));
        } catch (Exception e) {
            log.error("admin register failed", e);
            if (isDbConnectionError(e)) {
                return ResponseEntity.status(500).body(ApiResult.fail("注册失败：数据库连接异常，请检查数据库账号密码配置"));
            }
            return ResponseEntity.status(500).body(ApiResult.fail("注册失败，请稍后重试"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResult<Void>> forgotPassword(
            @RequestBody(required = false) Map<String, Object> body,
            HttpSession session) {
        try {
            if (body == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请求体不能为空"));
            }
            String captcha = String.valueOf(body.getOrDefault("captcha", ""));
            if (!captchaValidator.validateAndConsume(session, captcha)) {
                return ResponseEntity.badRequest().body(ApiResult.fail("验证码错误或已过期，请刷新后重试"));
            }
            String username = String.valueOf(body.getOrDefault("username", "")).trim();
            String email = String.valueOf(body.getOrDefault("email", "")).trim().toLowerCase();
            String newPassword = String.valueOf(body.getOrDefault("newPassword", ""));
            if (username.isEmpty() || email.isEmpty() || newPassword.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请填写账号、邮箱与新密码"));
            }
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(ApiResult.fail("新密码至少 6 位"));
            }
            AdminUser u = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getUsername, username));
            if (u == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("用户不存在"));
            }
            if (u.getEmail() == null || u.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResult.fail("该账号未绑定邮箱，无法自助找回，请联系超级管理员"));
            }
            if (!u.getEmail().trim().equalsIgnoreCase(email)) {
                return ResponseEntity.badRequest().body(ApiResult.fail("邮箱与账号不匹配"));
            }
            u.setPassword(newPassword);
            adminUserMapper.updateById(u);
            return ResponseEntity.ok(ApiResult.ok(null));
        } catch (Exception e) {
            log.error("admin forgot-password failed", e);
            if (isDbConnectionError(e)) {
                return ResponseEntity.status(500).body(ApiResult.fail("重置失败：数据库连接异常，请检查数据库账号密码配置"));
            }
            return ResponseEntity.status(500).body(ApiResult.fail("重置失败，请稍后重试"));
        }
    }

    private static boolean isDbConnectionError(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof CannotGetJdbcConnectionException) {
                return true;
            }
            String msg = cur.getMessage();
            if (msg != null && (msg.contains("Access denied for user") || msg.contains("CannotGetJdbcConnectionException"))) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }
}
