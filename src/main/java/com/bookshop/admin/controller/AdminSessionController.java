package com.bookshop.admin.controller;

import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.config.AdminAuthInterceptor;
import com.bookshop.admin.mapper.AdminUserMapper;
import com.bookshop.admin.util.CaptchaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminSessionController {

    private static final Logger log = LoggerFactory.getLogger(AdminSessionController.class);
    private static final int SESSION_SECONDS_REMEMBER = 7 * 24 * 60 * 60;
    private static final int SESSION_SECONDS_NORMAL = 30 * 60;
    private static final int AVATAR_MAX_LEN = 2_000_000;

    private final AdminUserMapper adminUserMapper;
    private final CaptchaValidator captchaValidator;

    public AdminSessionController(AdminUserMapper adminUserMapper, CaptchaValidator captchaValidator) {
        this.adminUserMapper = adminUserMapper;
        this.captchaValidator = captchaValidator;
    }

    @GetMapping("/session")
    public ApiResult<Map<String, Object>> session(HttpSession session) {
        boolean loggedIn = session != null && session.getAttribute(AdminAuthInterceptor.SESSION_ADMIN_ID) != null;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("loggedIn", loggedIn);
        return ApiResult.ok(data);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResult<Map<String, Object>>> me(HttpSession session) {
        try {
            Object idObj = session == null ? null : session.getAttribute(AdminAuthInterceptor.SESSION_ADMIN_ID);
            if (!(idObj instanceof Number)) {
                return ResponseEntity.status(401).body(ApiResult.fail("未登录或会话已过期"));
            }
            Long id = ((Number) idObj).longValue();
            com.bookshop.admin.entity.AdminUser u = adminUserMapper.selectById(id);
            if (u == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("管理员不存在"));
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", u.getId());
            data.put("username", u.getUsername());
            data.put("displayName", u.getDisplayName());
            data.put("avatarUrl", u.getAvatarUrl());
            return ResponseEntity.ok(ApiResult.ok(data));
        } catch (Exception e) {
            log.error("admin me failed", e);
            if (isDbConnectionError(e)) {
                return ResponseEntity.status(500).body(ApiResult.fail("读取个人信息失败：数据库连接异常"));
            }
            return ResponseEntity.status(500).body(ApiResult.fail("读取个人信息失败，请稍后重试"));
        }
    }

    @PostMapping("/me")
    public ResponseEntity<ApiResult<Map<String, Object>>> updateMe(
            @RequestBody(required = false) Map<String, Object> body,
            HttpSession session) {
        try {
            Object idObj = session == null ? null : session.getAttribute(AdminAuthInterceptor.SESSION_ADMIN_ID);
            if (!(idObj instanceof Number)) {
                return ResponseEntity.status(401).body(ApiResult.fail("未登录或会话已过期"));
            }
            if (body == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请求体不能为空"));
            }
            String avatarUrl = String.valueOf(body.getOrDefault("avatarUrl", "")).trim();
            if (avatarUrl.length() > AVATAR_MAX_LEN) {
                return ResponseEntity.badRequest().body(ApiResult.fail("头像数据过大"));
            }
            Long id = ((Number) idObj).longValue();
            com.bookshop.admin.entity.AdminUser u = adminUserMapper.selectById(id);
            if (u == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("管理员不存在"));
            }
            u.setAvatarUrl(avatarUrl);
            adminUserMapper.updateById(u);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", u.getId());
            data.put("username", u.getUsername());
            data.put("displayName", u.getDisplayName());
            data.put("avatarUrl", u.getAvatarUrl());
            return ResponseEntity.ok(ApiResult.ok(data));
        } catch (Exception e) {
            log.error("admin update me failed", e);
            if (isDbConnectionError(e)) {
                return ResponseEntity.status(500).body(ApiResult.fail("保存个人信息失败：数据库连接异常"));
            }
            return ResponseEntity.status(500).body(ApiResult.fail("保存个人信息失败，请稍后重试"));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResult<Void>> changePassword(
            @RequestBody(required = false) Map<String, Object> body,
            HttpSession session) {
        try {
            Object idObj = session == null ? null : session.getAttribute(AdminAuthInterceptor.SESSION_ADMIN_ID);
            if (!(idObj instanceof Number)) {
                return ResponseEntity.status(401).body(ApiResult.fail("未登录或会话已过期"));
            }
            if (body == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请求体不能为空"));
            }
            String oldPassword = String.valueOf(body.getOrDefault("oldPassword", ""));
            String newPassword = String.valueOf(body.getOrDefault("newPassword", ""));
            if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请输入旧密码和新密码"));
            }
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(ApiResult.fail("新密码至少 6 位"));
            }
            Long id = ((Number) idObj).longValue();
            com.bookshop.admin.entity.AdminUser u = adminUserMapper.selectById(id);
            if (u == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("管理员不存在"));
            }
            if (!oldPassword.equals(u.getPassword())) {
                return ResponseEntity.badRequest().body(ApiResult.fail("旧密码错误"));
            }
            u.setPassword(newPassword);
            adminUserMapper.updateById(u);
            return ResponseEntity.ok(ApiResult.ok(null));
        } catch (Exception e) {
            log.error("admin change-password failed", e);
            if (isDbConnectionError(e)) {
                return ResponseEntity.status(500).body(ApiResult.fail("修改密码失败：数据库连接异常"));
            }
            return ResponseEntity.status(500).body(ApiResult.fail("修改密码失败，请稍后重试"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResult<Map<String, Object>>> login(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        try {
            if (body == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请求体不能为空"));
            }
            String captcha = String.valueOf(body.getOrDefault("captcha", ""));
            if (!captchaValidator.validateAndConsume(session, captcha)) {
                return ResponseEntity.badRequest().body(ApiResult.fail("验证码错误或已过期，请刷新后重试"));
            }
            String username = String.valueOf(body.getOrDefault("username", "")).trim();
            String password = String.valueOf(body.getOrDefault("password", ""));
            if (username.isEmpty() || password.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResult.fail("用户名和密码不能为空"));
            }
            boolean rememberMe = parseRememberMe(body.get("rememberMe"));
            return adminUserMapper.findByUsername(username)
                    .filter(u -> password.equals(u.getPassword()))
                    .map(u -> {
                        session.setAttribute(AdminAuthInterceptor.SESSION_ADMIN_ID, u.getId());
                        session.setMaxInactiveInterval(rememberMe ? SESSION_SECONDS_REMEMBER : SESSION_SECONDS_NORMAL);
                        Map<String, Object> data = new LinkedHashMap<>();
                        data.put("id", u.getId());
                        data.put("username", u.getUsername());
                        data.put("displayName", u.getDisplayName());
                        return ResponseEntity.ok(ApiResult.ok(data));
                    })
                    .orElseGet(() -> ResponseEntity.status(401).body(ApiResult.fail("用户名或密码错误")));
        } catch (Exception e) {
            log.error("admin login failed", e);
            if (isDbConnectionError(e)) {
                return ResponseEntity.status(500).body(ApiResult.fail("登录失败：数据库连接异常，请检查数据库账号密码配置"));
            }
            return ResponseEntity.status(500).body(ApiResult.fail("服务器错误，请稍后重试"));
        }
    }

    private static boolean parseRememberMe(Object raw) {
        if (raw == null) {
            return false;
        }
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        return "true".equalsIgnoreCase(String.valueOf(raw).trim());
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

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s != null) {
            s.invalidate();
        }
        return ApiResult.ok(null);
    }
}
