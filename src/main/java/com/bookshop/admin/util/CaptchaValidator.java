package com.bookshop.admin.util;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;

@Component
public class CaptchaValidator {

    public static final String ADMIN_SESSION_CODE = "ADMIN_CAPTCHA";
    public static final String ADMIN_SESSION_TIME = "ADMIN_CAPTCHA_TIME";
    public static final String USER_SESSION_CODE = "USER_CAPTCHA";
    public static final String USER_SESSION_TIME = "USER_CAPTCHA_TIME";

    /** @deprecated 使用 {@link #ADMIN_SESSION_CODE} */
    public static final String SESSION_CODE = ADMIN_SESSION_CODE;
    /** @deprecated 使用 {@link #ADMIN_SESSION_TIME} */
    public static final String SESSION_TIME = ADMIN_SESSION_TIME;

    private static final long TTL_MS = 180_000;

    public boolean validateAndConsumeAdmin(HttpSession session, String userInput) {
        return validateAndConsume(session, userInput, ADMIN_SESSION_CODE, ADMIN_SESSION_TIME);
    }

    public boolean validateAndConsumeUser(HttpSession session, String userInput) {
        return validateAndConsume(session, userInput, USER_SESSION_CODE, USER_SESSION_TIME);
    }

    /** 兼容旧调用，等同管理员验证码 */
    public boolean validateAndConsume(HttpSession session, String userInput) {
        return validateAndConsumeAdmin(session, userInput);
    }

    private boolean validateAndConsume(HttpSession session, String userInput, String codeKey, String timeKey) {
        if (session == null) {
            return false;
        }
        Object codeObj = session.getAttribute(codeKey);
        Object timeObj = session.getAttribute(timeKey);
        session.removeAttribute(codeKey);
        session.removeAttribute(timeKey);
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
