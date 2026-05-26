package com.bookshop.admin.controller;

import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.dto.GuestbookMessageRow;
import com.bookshop.admin.entity.GuestbookMessage;
import com.bookshop.admin.mapper.GuestbookMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user/guestbook")
public class UserGuestbookController {

    private static final Logger log = LoggerFactory.getLogger(UserGuestbookController.class);

    private static final int CONTENT_MIN = 2;
    private static final int CONTENT_MAX = 500;

    private final GuestbookMessageMapper guestbookMessageMapper;

    public UserGuestbookController(GuestbookMessageMapper guestbookMessageMapper) {
        this.guestbookMessageMapper = guestbookMessageMapper;
    }

    @PostMapping("/messages")
    public ApiResult<GuestbookMessageRow> post(@RequestBody(required = false) Map<String, Object> body, HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) {
                return ApiResult.fail("未登录或会话已过期，请先登录");
            }

            String content = body == null ? "" : String.valueOf(body.getOrDefault("content", "")).trim();
            if (content.length() < CONTENT_MIN) {
                return ApiResult.fail("留言内容至少 " + CONTENT_MIN + " 个字");
            }
            if (content.length() > CONTENT_MAX) {
                return ApiResult.fail("留言内容不能超过 " + CONTENT_MAX + " 字");
            }

            GuestbookMessage row = new GuestbookMessage();
            row.setCustomerId(userId);
            row.setContent(content);
            row.setStatus("VISIBLE");
            row.setCreatedAt(LocalDateTime.now());
            guestbookMessageMapper.insert(row);

            GuestbookMessageRow created = guestbookMessageMapper.selectVisibleById(row.getId());
            if (created != null) {
                created.setMine(true);
            }
            return ApiResult.ok(created);
        } catch (DataAccessException e) {
            log.error("post guestbook failed", e);
            return ApiResult.fail(dbHint(e));
        } catch (Exception e) {
            log.error("post guestbook failed", e);
            return ApiResult.fail("发布留言失败，请稍后重试");
        }
    }

    @DeleteMapping("/messages/{id}")
    public ApiResult<Map<String, Object>> delete(@PathVariable Long id, HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) {
                return ApiResult.fail("未登录或会话已过期，请先登录");
            }
            if (id == null || id <= 0) {
                return ApiResult.fail("无效的留言编号");
            }

            GuestbookMessage existing = guestbookMessageMapper.selectById(id);
            if (existing == null) {
                return ApiResult.fail("留言不存在");
            }
            if (!userId.equals(existing.getCustomerId())) {
                return ApiResult.fail("只能删除自己的留言");
            }

            guestbookMessageMapper.deleteById(id);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", "已删除");
            return ApiResult.ok(data);
        } catch (DataAccessException e) {
            log.error("delete guestbook failed", e);
            return ApiResult.fail(dbHint(e));
        } catch (Exception e) {
            log.error("delete guestbook failed", e);
            return ApiResult.fail("删除失败，请稍后重试");
        }
    }

    private static Long getUserId(HttpSession session) {
        Object idObj = session == null ? null : session.getAttribute("USER_ID");
        if (!(idObj instanceof Number)) {
            return null;
        }
        return ((Number) idObj).longValue();
    }

    private static String dbHint(DataAccessException e) {
        String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        if (msg != null && msg.contains("guestbook_message")) {
            return "留言表未就绪，请重启后端或执行 sql/patch_guestbook_message.sql";
        }
        return "数据库操作失败，请稍后重试";
    }
}
