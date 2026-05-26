package com.bookshop.admin.controller;

import com.bookshop.admin.util.CaptchaUtil;
import com.bookshop.admin.util.CaptchaValidator;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/admin")
public class AdminCaptchaController {

    @GetMapping("/captcha")
    public ResponseEntity<byte[]> captcha(HttpSession session) throws Exception {
        String code = CaptchaUtil.randomCode();
        session.setAttribute(CaptchaValidator.ADMIN_SESSION_CODE, code.toLowerCase());
        session.setAttribute(CaptchaValidator.ADMIN_SESSION_TIME, System.currentTimeMillis());
        byte[] svg = CaptchaUtil.svgBytes(code);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(svg);
    }
}
