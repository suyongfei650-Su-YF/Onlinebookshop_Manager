package com.bookshop.admin.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.config.AdminAuthInterceptor;
import com.bookshop.admin.dto.GuestbookMessageRow;
import com.bookshop.admin.entity.GuestbookMessage;
import com.bookshop.admin.mapper.GuestbookMessageMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminGuestbookController {

    private static final int REPLY_MAX = 500;

    private final GuestbookMessageMapper guestbookMessageMapper;

    public AdminGuestbookController(GuestbookMessageMapper guestbookMessageMapper) {
        this.guestbookMessageMapper = guestbookMessageMapper;
    }

    @GetMapping("/guestbook/messages")
    public ResponseEntity<ApiResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String replyFilter) {
        try {
            size = Math.min(Math.max(size, 1), 100);
            page = Math.max(page, 1);
            String kw = keyword == null || keyword.trim().isEmpty() ? null : keyword.trim();
            String filter = normalizeReplyFilter(replyFilter);
            int offset = (page - 1) * size;
            long total = guestbookMessageMapper.countAdmin(kw, filter);
            List<GuestbookMessageRow> list = guestbookMessageMapper.selectAdminPage(offset, size, kw, filter);
            long pendingCount = guestbookMessageMapper.countAdminPending();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("list", list);
            data.put("total", total);
            data.put("page", page);
            data.put("size", size);
            data.put("pendingCount", pendingCount);
            return ResponseEntity.ok(ApiResult.ok(data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }

    @PutMapping("/guestbook/messages/{id}/reply")
    public ResponseEntity<ApiResult<GuestbookMessageRow>> reply(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        try {
            GuestbookMessage msg = guestbookMessageMapper.selectById(id);
            if (msg == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("留言不存在"));
            }

            boolean clear = Boolean.TRUE.equals(body.get("clear"));
            String replyText = null;
            Object replyObj = body.get("reply");
            if (replyObj != null) {
                replyText = replyObj.toString().trim();
            }

            LambdaUpdateWrapper<GuestbookMessage> uw = new LambdaUpdateWrapper<>();
            uw.eq(GuestbookMessage::getId, id);

            if (clear || replyText == null || replyText.isEmpty()) {
                uw.set(GuestbookMessage::getAdminReply, null)
                        .set(GuestbookMessage::getAdminReplyAt, null)
                        .set(GuestbookMessage::getAdminReplierId, null);
            } else {
                if (replyText.length() > REPLY_MAX) {
                    return ResponseEntity.badRequest().body(ApiResult.fail("回复内容不能超过 " + REPLY_MAX + " 字"));
                }
                Long adminId = adminIdFromSession(session);
                if (adminId == null) {
                    return ResponseEntity.status(401).body(ApiResult.fail("未登录或会话已过期"));
                }
                uw.set(GuestbookMessage::getAdminReply, replyText)
                        .set(GuestbookMessage::getAdminReplyAt, LocalDateTime.now())
                        .set(GuestbookMessage::getAdminReplierId, adminId);
            }

            guestbookMessageMapper.update(null, uw);
            GuestbookMessageRow row = guestbookMessageMapper.selectAdminById(id);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("留言不存在"));
            }
            return ResponseEntity.ok(ApiResult.ok(row));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }

    private static String normalizeReplyFilter(String replyFilter) {
        if (replyFilter == null) {
            return null;
        }
        String f = replyFilter.trim().toLowerCase();
        if ("pending".equals(f) || "replied".equals(f)) {
            return f;
        }
        return null;
    }

    private static Long adminIdFromSession(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object idObj = session.getAttribute(AdminAuthInterceptor.SESSION_ADMIN_ID);
        if (!(idObj instanceof Number)) {
            return null;
        }
        return ((Number) idObj).longValue();
    }
}
