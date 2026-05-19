package com.bookshop.admin.util;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;

@Component
public class CaptchaValidator {

    public static final String SESSION_CODE = "ADMIN_CAPTCHA";
    public static final String SESSION_TIME = "ADMIN_CAPTCHA_TIME";
    private static final long TTL_MS = 180_000;

    /** 校验并清除 Session 中的验证码（无论对错都清除，需重新获取图片） */
    public boolean validateAndConsume(HttpSession session, String userInput) {
        if (session == null) {
            return false;
        }
        Object codeObj = session.getAttribute(SESSION_CODE);
        Object timeObj = session.getAttribute(SESSION_TIME);
        session.removeAttribute(SESSION_CODE);
        session.removeAttribute(SESSION_TIME);
        if (!(codeObj instanceof String) || !(timeObj instanceof Long)) {
            return false;
        }
        if (System.currentTimeMillis() - (Long) timeObj > TTL_MS) {
            return false;
        }
        String expect = ((String) codeObj).trim().toLowerCase();
        String got = userInput == null ? "" : userInput.trim().toLowerCase();
        return !expect.isEmpty() && expect.equals(got);
    }
}
