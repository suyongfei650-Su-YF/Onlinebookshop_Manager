package com.bookshop.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.dto.AdminCustomerRow;
import com.bookshop.admin.entity.Customer;
import com.bookshop.admin.mapper.CustomerMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/admin")
public class AdminCustomersController {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^1\\d{10}$");
    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9_]{3,32}$");

    private final CustomerMapper customerMapper;

    public AdminCustomersController(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roleTag) {
        try {
            size = Math.min(Math.max(size, 1), 100);
            page = Math.max(page, 1);
            String kw = trimOrNull(keyword);
            String st = trimOrNull(status);
            String tag = trimOrNull(roleTag);
            int offset = (page - 1) * size;
            long total = customerMapper.countAdminCustomers(kw, st, tag);
            List<AdminCustomerRow> list = customerMapper.selectAdminCustomerPage(kw, st, tag, offset, size);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("list", list);
            data.put("total", total);
            data.put("page", page);
            data.put("size", size);
            data.put("statusCounts", toStatusCountMap(customerMapper.countCustomersByStatus()));
            return ResponseEntity.ok(ApiResult.ok(data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<ApiResult<AdminCustomerRow>> detail(@PathVariable Long id) {
        try {
            AdminCustomerRow row = customerMapper.selectAdminCustomerById(id);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("用户不存在"));
            }
            return ResponseEntity.ok(ApiResult.ok(row));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }

    @PostMapping("/customers")
    public ResponseEntity<ApiResult<AdminCustomerRow>> create(@RequestBody(required = false) Map<String, Object> body) {
        try {
            if (body == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请求体不能为空"));
            }
            String nickname = parseStr(body.get("nickname"));
            if (nickname == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("昵称不能为空"));
            }
            String username = parseStr(body.get("username"));
            String email = parseStr(body.get("email"));
            String phone = parseStr(body.get("phone"));
            String password = parseStr(body.get("password"));
            if (password == null) {
                password = "123456";
            }
            if (username == null && email == null && phone == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请至少填写用户名、邮箱或手机号之一"));
            }
            String err = validateUniqueFields(null, username, email, phone);
            if (err != null) {
                return ResponseEntity.badRequest().body(ApiResult.fail(err));
            }
            err = validateFieldFormats(username, email, phone);
            if (err != null) {
                return ResponseEntity.badRequest().body(ApiResult.fail(err));
            }

            Customer c = new Customer();
            c.setNickname(nickname);
            c.setUsername(username);
            c.setEmail(email);
            c.setPhone(phone);
            c.setPassword(password);
            c.setRoleTag(parseStr(body.get("roleTag"), "普通读者"));
            c.setStatus(parseStatus(body.get("status"), "ACTIVE"));
            c.setCreatedAt(LocalDateTime.now());
            customerMapper.insert(c);

            AdminCustomerRow row = customerMapper.selectAdminCustomerById(c.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(row));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("新增用户失败: " + e.getMessage()));
        }
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<ApiResult<AdminCustomerRow>> update(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            Customer old = customerMapper.selectById(id);
            if (old == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("用户不存在"));
            }
            if (body == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请求体不能为空"));
            }

            String nickname = body.containsKey("nickname") ? parseStr(body.get("nickname")) : old.getNickname();
            if (nickname == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("昵称不能为空"));
            }
            String username = body.containsKey("username") ? parseStr(body.get("username")) : old.getUsername();
            String email = body.containsKey("email") ? parseStr(body.get("email")) : old.getEmail();
            String phone = body.containsKey("phone") ? parseStr(body.get("phone")) : old.getPhone();

            String err = validateUniqueFields(id, username, email, phone);
            if (err != null) {
                return ResponseEntity.badRequest().body(ApiResult.fail(err));
            }
            err = validateFieldFormats(username, email, phone);
            if (err != null) {
                return ResponseEntity.badRequest().body(ApiResult.fail(err));
            }

            old.setNickname(nickname);
            old.setUsername(username);
            old.setEmail(email);
            old.setPhone(phone);
            if (body.containsKey("roleTag")) {
                old.setRoleTag(parseStr(body.get("roleTag"), old.getRoleTag()));
            }
            if (body.containsKey("status")) {
                old.setStatus(parseStatus(body.get("status"), old.getStatus()));
            }
            String newPassword = parseStr(body.get("password"));
            if (newPassword != null) {
                old.setPassword(newPassword);
            }
            old.setUpdatedAt(LocalDateTime.now());
            customerMapper.updateById(old);

            AdminCustomerRow row = customerMapper.selectAdminCustomerById(id);
            return ResponseEntity.ok(ApiResult.ok(row));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("更新用户失败: " + e.getMessage()));
        }
    }

    @PostMapping("/customers/{id}/toggle-status")
    public ResponseEntity<ApiResult<AdminCustomerRow>> toggleStatus(@PathVariable Long id) {
        try {
            Customer c = customerMapper.selectById(id);
            if (c == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("用户不存在"));
            }
            String next = "ACTIVE".equalsIgnoreCase(c.getStatus()) ? "DISABLED" : "ACTIVE";
            c.setStatus(next);
            c.setUpdatedAt(LocalDateTime.now());
            customerMapper.updateById(c);
            AdminCustomerRow row = customerMapper.selectAdminCustomerById(id);
            return ResponseEntity.ok(ApiResult.ok(row));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("更新状态失败: " + e.getMessage()));
        }
    }

    private static String trimOrNull(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        return s.isEmpty() ? null : s;
    }

    private static String parseStr(Object raw) {
        if (raw == null) return null;
        String s = String.valueOf(raw).trim();
        return s.isEmpty() ? null : s;
    }

    private static String parseStr(Object raw, String fallback) {
        String s = parseStr(raw);
        return s != null ? s : fallback;
    }

    private static String parseStatus(Object raw, String fallback) {
        String s = parseStr(raw);
        if (s == null) return fallback;
        if ("ACTIVE".equalsIgnoreCase(s) || "DISABLED".equalsIgnoreCase(s)) {
            return s.toUpperCase();
        }
        return fallback;
    }

    private static String validateFieldFormats(String username, String email, String phone) {
        if (username != null && !USERNAME.matcher(username).matches()) {
            return "用户名格式不正确（3-32位字母/数字/下划线）";
        }
        if (email != null && !EMAIL.matcher(email).matches()) {
            return "邮箱格式不正确";
        }
        if (phone != null && !PHONE.matcher(phone).matches()) {
            return "手机号格式不正确";
        }
        return null;
    }

    private String validateUniqueFields(Long excludeId, String username, String email, String phone) {
        if (username != null && countByField(excludeId, "username", username) > 0) {
            return "用户名已被占用";
        }
        if (email != null && countByField(excludeId, "email", email) > 0) {
            return "邮箱已被占用";
        }
        if (phone != null && countByField(excludeId, "phone", phone) > 0) {
            return "手机号已被占用";
        }
        return null;
    }

    private long countByField(Long excludeId, String field, String value) {
        LambdaQueryWrapper<Customer> q = new LambdaQueryWrapper<>();
        if ("username".equals(field)) {
            q.eq(Customer::getUsername, value);
        } else if ("email".equals(field)) {
            q.eq(Customer::getEmail, value);
        } else if ("phone".equals(field)) {
            q.eq(Customer::getPhone, value);
        }
        if (excludeId != null) {
            q.ne(Customer::getId, excludeId);
        }
        Long cnt = customerMapper.selectCount(q);
        return cnt == null ? 0 : cnt;
    }

    private static Map<String, Long> toStatusCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new HashMap<>();
        if (rows == null) return map;
        for (Map<String, Object> row : rows) {
            Object status = row.get("status");
            Object cnt = row.get("cnt");
            if (status != null && cnt instanceof Number) {
                map.put(String.valueOf(status), ((Number) cnt).longValue());
            }
        }
        return map;
    }
}
