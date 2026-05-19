package com.bookshop.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.entity.Customer;
import com.bookshop.admin.mapper.CustomerMapper;
import com.bookshop.admin.util.CaptchaValidator;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/user")
public class UserAuthController {

    private final CustomerMapper customerMapper;
    private final CaptchaValidator captchaValidator;

    public UserAuthController(CustomerMapper customerMapper, CaptchaValidator captchaValidator) {
        this.customerMapper = customerMapper;
        this.captchaValidator = captchaValidator;
    }

    public static class RegisterReq {
        public String account;
        public String nickname;
        public String password;
        public String captcha;
    }

    public static class LoginReq {
        public String account;
        public String password;
        public String captcha;
    }

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^1\\d{10}$");

    @PostMapping("/register")
    public ApiResult<Customer> register(@RequestBody RegisterReq req, HttpSession session) {
        try {
            if (req == null) return ApiResult.fail("请求为空");
            String account = req.account == null ? "" : req.account.trim();
            String nickname = req.nickname == null ? "" : req.nickname.trim();
            String password = req.password == null ? "" : req.password.trim();
            if (account.isEmpty()) return ApiResult.fail("请输入账号/邮箱/手机号");
            if (nickname.isEmpty()) return ApiResult.fail("请输入网名");
            if (password.isEmpty()) return ApiResult.fail("请输入密码");
            if (!captchaValidator.validateAndConsume(session, req.captcha)) return ApiResult.fail("验证码错误或已过期");

            Customer c = new Customer();
            c.setNickname(nickname);
            c.setStatus("ACTIVE");
            c.setRoleTag("普通读者");
            c.setPassword(password);

            if (EMAIL.matcher(account).matches()) c.setEmail(account);
            else if (PHONE.matcher(account).matches()) c.setPhone(account);
            else c.setUsername(account);

            // 账号唯一性：任一字段被占用都拒绝
            Long cnt = customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                    .eq(c.getUsername() != null, Customer::getUsername, c.getUsername())
                    .or().eq(c.getEmail() != null, Customer::getEmail, c.getEmail())
                    .or().eq(c.getPhone() != null, Customer::getPhone, c.getPhone()));
            if (cnt != null && cnt > 0) return ApiResult.fail("账号已被注册");

            customerMapper.insert(c);
            session.setAttribute("USER_ID", c.getId());
            return ApiResult.ok(c);
        } catch (Exception e) {
            return ApiResult.fail("注册失败，请稍后重试");
        }
    }

    @PostMapping("/login")
    public ApiResult<Customer> login(@RequestBody LoginReq req, HttpSession session) {
        try {
            if (req == null) return ApiResult.fail("请求为空");
            String account = req.account == null ? "" : req.account.trim();
            String password = req.password == null ? "" : req.password.trim();
            if (account.isEmpty() || password.isEmpty()) return ApiResult.fail("请输入账号和密码");
            if (!captchaValidator.validateAndConsume(session, req.captcha)) return ApiResult.fail("验证码错误或已过期");

            LambdaQueryWrapper<Customer> q = new LambdaQueryWrapper<>();
            if (EMAIL.matcher(account).matches()) q.eq(Customer::getEmail, account);
            else if (PHONE.matcher(account).matches()) q.eq(Customer::getPhone, account);
            else q.eq(Customer::getUsername, account);

            Customer c = customerMapper.selectOne(q);
            if (c == null) return ApiResult.fail("账号不存在");
            if (!"ACTIVE".equalsIgnoreCase(c.getStatus())) return ApiResult.fail("账号已被禁用");
            if (c.getPassword() == null || !c.getPassword().equals(password)) return ApiResult.fail("密码错误");

            session.setAttribute("USER_ID", c.getId());
            return ApiResult.ok(c);
        } catch (Exception e) {
            return ApiResult.fail("登录失败，请稍后重试");
        }
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ApiResult.ok(null);
    }
}

