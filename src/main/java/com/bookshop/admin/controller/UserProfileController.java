package com.bookshop.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.entity.Customer;
import com.bookshop.admin.mapper.CustomerMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private static final Logger log = LoggerFactory.getLogger(UserProfileController.class);
    private static final int AVATAR_MAX_LEN = 2_000_000;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^1\\d{10}$");
    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9_]{3,32}$");

    private final CustomerMapper customerMapper;

    public UserProfileController(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    private static Long getUserId(HttpSession session) {
        Object idObj = session == null ? null : session.getAttribute("USER_ID");
        if (!(idObj instanceof Number)) {
            return null;
        }
        return ((Number) idObj).longValue();
    }

    private static Customer sanitize(Customer c) {
        if (c == null) return null;
        c.setPassword(null);
        return c;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResult<Customer>> me(HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) return ResponseEntity.status(401).body(ApiResult.fail("未登录或会话已过期"));
            Customer c = customerMapper.selectById(userId);
            if (c == null) return ResponseEntity.status(404).body(ApiResult.fail("用户不存在"));
            return ResponseEntity.ok(ApiResult.ok(sanitize(c)));
        } catch (Exception e) {
            log.error("user me failed", e);
            return ResponseEntity.status(500).body(ApiResult.fail("读取个人资料失败，请稍后重试"));
        }
    }

    public static class UpdateMeReq {
        public String nickname;
        public String username;
        public String email;
        public String phone;
        public String avatarUrl;
    }

    @PostMapping("/me")
    public ResponseEntity<ApiResult<Customer>> updateMe(
            @RequestBody(required = false) UpdateMeReq req,
            HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) return ResponseEntity.status(401).body(ApiResult.fail("未登录或会话已过期"));
            if (req == null) return ResponseEntity.badRequest().body(ApiResult.fail("请求体不能为空"));

            Customer c = customerMapper.selectById(userId);
            if (c == null) return ResponseEntity.status(404).body(ApiResult.fail("用户不存在"));

            boolean hasAnyField =
                    req.nickname != null || req.username != null || req.email != null || req.phone != null || req.avatarUrl != null;
            if (!hasAnyField) {
                return ResponseEntity.badRequest().body(ApiResult.fail("未提供任何可修改字段"));
            }

            String nickname = req.nickname == null ? null : req.nickname.trim();
            String username = req.username == null ? null : req.username.trim();
            String email = req.email == null ? null : req.email.trim();
            String phone = req.phone == null ? null : req.phone.trim();
            String avatarUrl = req.avatarUrl == null ? null : req.avatarUrl.trim();

            if (nickname != null && nickname.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResult.fail("昵称不能为空"));
            }

            if (username != null && !username.isEmpty() && !USERNAME.matcher(username).matches()) {
                return ResponseEntity.badRequest().body(ApiResult.fail("用户名格式不正确（3-32位字母/数字/下划线）"));
            }
            if (email != null && !email.isEmpty() && !EMAIL.matcher(email).matches()) {
                return ResponseEntity.badRequest().body(ApiResult.fail("邮箱格式不正确"));
            }
            if (phone != null && !phone.isEmpty() && !PHONE.matcher(phone).matches()) {
                return ResponseEntity.badRequest().body(ApiResult.fail("手机号格式不正确"));
            }
            if (avatarUrl != null && avatarUrl.length() > AVATAR_MAX_LEN) {
                return ResponseEntity.badRequest().body(ApiResult.fail("头像数据过大"));
            }

            // 唯一性校验（排除自己）——分项校验，便于前端提示具体字段
            if (username != null && !username.isEmpty()) {
                Long cnt = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                        .ne(Customer::getId, userId)
                        .eq(Customer::getUsername, username));
                if (cnt != null && cnt > 0) {
                    return ResponseEntity.badRequest().body(ApiResult.fail("用户名已被占用"));
                }
            }
            if (email != null && !email.isEmpty()) {
                Long cnt = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                        .ne(Customer::getId, userId)
                        .eq(Customer::getEmail, email));
                if (cnt != null && cnt > 0) {
                    return ResponseEntity.badRequest().body(ApiResult.fail("邮箱已被占用"));
                }
            }
            if (phone != null && !phone.isEmpty()) {
                Long cnt = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                        .ne(Customer::getId, userId)
                        .eq(Customer::getPhone, phone));
                if (cnt != null && cnt > 0) {
                    return ResponseEntity.badRequest().body(ApiResult.fail("手机号已被占用"));
                }
            }

            if (nickname != null) {
                c.setNickname(nickname);
            }
            if (username != null) {
                c.setUsername(username.isEmpty() ? null : username);
            }
            if (email != null) {
                c.setEmail(email.isEmpty() ? null : email);
            }
            if (phone != null) {
                c.setPhone(phone.isEmpty() ? null : phone);
            }
            if (avatarUrl != null) {
                c.setAvatarUrl(avatarUrl.isEmpty() ? null : avatarUrl);
            }

            customerMapper.updateById(c);
            Customer fresh = customerMapper.selectById(userId);
            return ResponseEntity.ok(ApiResult.ok(sanitize(fresh)));
        } catch (Exception e) {
            log.error("user update me failed", e);
            return ResponseEntity.status(500).body(ApiResult.fail("保存个人资料失败，请稍后重试"));
        }
    }
}

