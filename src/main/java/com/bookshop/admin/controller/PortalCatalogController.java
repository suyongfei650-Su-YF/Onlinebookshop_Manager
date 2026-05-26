package com.bookshop.admin.controller;

import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.dto.GuestbookMessageRow;
import com.bookshop.admin.dto.PortalBookDetail;
import com.bookshop.admin.dto.PortalCategoryRow;
import com.bookshop.admin.entity.Book;
import com.bookshop.admin.mapper.BookMapper;
import com.bookshop.admin.mapper.GuestbookMessageMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/portal")
public class PortalCatalogController {

    private static final Set<String> SORT_WHITELIST = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("default", "price_asc", "price_desc", "title")));

    private static final int GUESTBOOK_DEFAULT_SIZE = 20;
    private static final int GUESTBOOK_MAX_SIZE = 50;

    private final BookMapper bookMapper;
    private final GuestbookMessageMapper guestbookMessageMapper;

    public PortalCatalogController(BookMapper bookMapper, GuestbookMessageMapper guestbookMessageMapper) {
        this.bookMapper = bookMapper;
        this.guestbookMessageMapper = guestbookMessageMapper;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResult<List<PortalCategoryRow>>> categories() {
        try {
            return ResponseEntity.ok(ApiResult.ok(bookMapper.selectPortalCategoryStats()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }

    @GetMapping("/books")
    public ResponseEntity<ApiResult<Map<String, Object>>> books(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(defaultValue = "default") String sort) {
        try {
            size = Math.min(Math.max(size, 1), 48);
            page = Math.max(page, 1);
            String kw = keyword == null || keyword.trim().isEmpty() ? null : keyword.trim();
            String sortKey = SORT_WHITELIST.contains(sort) ? sort : "default";
            int offset = (page - 1) * size;
            long total = bookMapper.countPortal(kw, categoryId, priceMin, priceMax);
            List<Book> list = bookMapper.selectPortalPage(kw, categoryId, priceMin, priceMax, sortKey, offset, size);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("list", list);
            data.put("total", total);
            data.put("page", page);
            data.put("size", size);
            return ResponseEntity.ok(ApiResult.ok(data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }

    @GetMapping("/guestbook/messages")
    public ResponseEntity<ApiResult<Map<String, Object>>> guestbookMessages(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            HttpSession session) {
        try {
            int p = Math.max(1, page);
            int s = Math.min(GUESTBOOK_MAX_SIZE, Math.max(1, size <= 0 ? GUESTBOOK_DEFAULT_SIZE : size));
            int offset = (p - 1) * s;

            Long userId = sessionUserId(session);
            List<GuestbookMessageRow> rows = guestbookMessageMapper.selectVisiblePage(offset, s);
            if (rows == null) {
                rows = new ArrayList<>();
            }
            if (userId != null) {
                for (GuestbookMessageRow row : rows) {
                    if (row != null && row.getCustomerId() != null) {
                        row.setMine(userId.equals(row.getCustomerId()));
                    }
                }
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("list", rows);
            data.put("total", guestbookMessageMapper.countVisible());
            data.put("page", p);
            data.put("size", s);
            data.put("loggedIn", userId != null);
            return ResponseEntity.ok(ApiResult.ok(data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("加载留言失败，请稍后重试"));
        }
    }

    @GetMapping("/books/{id}")
    public ResponseEntity<ApiResult<PortalBookDetail>> bookDetail(@PathVariable Long id) {
        try {
            PortalBookDetail book = bookMapper.selectPortalBookDetail(id);
            if (book == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("图书不存在或已下架"));
            }
            return ResponseEntity.ok(ApiResult.ok(book));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }

    private static Long sessionUserId(HttpSession session) {
        Object idObj = session == null ? null : session.getAttribute("USER_ID");
        if (!(idObj instanceof Number)) {
            return null;
        }
        return ((Number) idObj).longValue();
    }
}
