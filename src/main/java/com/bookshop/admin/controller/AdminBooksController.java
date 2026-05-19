package com.bookshop.admin.controller;

import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.entity.Book;
import com.bookshop.admin.mapper.BookMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminBooksController {

    private final BookMapper bookMapper;

    public AdminBooksController(BookMapper bookMapper) {
        this.bookMapper = bookMapper;
    }

    @GetMapping("/books")
    public ResponseEntity<ApiResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId) {
        try {
            size = Math.min(size, 100);
            int offset = (page - 1) * size;
            String kw = keyword == null || keyword.trim().isEmpty() ? null : keyword.trim();
            String st = status == null || status.trim().isEmpty() ? null : status.trim();
            long total = bookMapper.countByKeyword(kw, st, categoryId);
            List<Book> list = bookMapper.selectPageByKeyword(kw, st, categoryId, offset, size);
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

    @PostMapping("/books")
    public ResponseEntity<ApiResult<Book>> create(@RequestBody(required = false) Map<String, Object> body) {
        try {
            if (body == null) return ResponseEntity.badRequest().body(ApiResult.fail("请求体不能为空"));
            Book book = parseBookFromBody(body, null);
            if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResult.fail("书名不能为空"));
            }
            if (book.getPrice() == null) book.setPrice(BigDecimal.ZERO);
            if (book.getStock() == null) book.setStock(0);
            if (book.getStatus() == null || book.getStatus().trim().isEmpty()) book.setStatus("ON_SHELF");
            bookMapper.insert(book);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(book));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("新增图书失败: " + e.getMessage()));
        }
    }

    @PutMapping("/books/{id}")
    public ResponseEntity<ApiResult<Book>> update(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            Book old = bookMapper.selectById(id);
            if (old == null) return ResponseEntity.status(404).body(ApiResult.fail("图书不存在"));
            if (body == null) return ResponseEntity.badRequest().body(ApiResult.fail("请求体不能为空"));
            Book next = parseBookFromBody(body, old);
            next.setId(id);
            bookMapper.updateById(next);
            return ResponseEntity.ok(ApiResult.ok(next));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("更新图书失败: " + e.getMessage()));
        }
    }

    @PostMapping("/books/{id}/toggle-status")
    public ResponseEntity<ApiResult<Book>> toggleStatus(@PathVariable Long id) {
        try {
            Book old = bookMapper.selectById(id);
            if (old == null) return ResponseEntity.status(404).body(ApiResult.fail("图书不存在"));
            old.setStatus("ON_SHELF".equalsIgnoreCase(old.getStatus()) ? "OFF_SHELF" : "ON_SHELF");
            bookMapper.updateById(old);
            return ResponseEntity.ok(ApiResult.ok(old));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("更新状态失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<ApiResult<Void>> delete(@PathVariable Long id) {
        try {
            Book old = bookMapper.selectById(id);
            if (old == null) return ResponseEntity.status(404).body(ApiResult.fail("图书不存在"));
            bookMapper.deleteById(id);
            return ResponseEntity.ok(ApiResult.ok(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("删除图书失败: " + e.getMessage()));
        }
    }

    private static Book parseBookFromBody(Map<String, Object> body, Book fallback) {
        Book b = new Book();
        b.setCategoryId(parseLong(body.get("categoryId"), fallback == null ? null : fallback.getCategoryId()));
        b.setTitle(parseStr(body.get("title"), fallback == null ? null : fallback.getTitle()));
        b.setAuthor(parseStr(body.get("author"), fallback == null ? null : fallback.getAuthor()));
        b.setIsbn(parseStr(body.get("isbn"), fallback == null ? null : fallback.getIsbn()));
        b.setPrice(parseDecimal(body.get("price"), fallback == null ? null : fallback.getPrice()));
        b.setStock(parseInt(body.get("stock"), fallback == null ? null : fallback.getStock()));
        b.setStatus(parseStr(body.get("status"), fallback == null ? null : fallback.getStatus()));
        b.setCoverUrl(parseStr(body.get("coverUrl"), fallback == null ? null : fallback.getCoverUrl()));
        b.setDescription(parseStr(body.get("description"), fallback == null ? null : fallback.getDescription()));
        return b;
    }

    private static String parseStr(Object raw, String fallback) {
        if (raw == null) return fallback;
        String s = String.valueOf(raw).trim();
        return s.isEmpty() ? fallback : s;
    }

    private static Integer parseInt(Object raw, Integer fallback) {
        if (raw == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Long parseLong(Object raw, Long fallback) {
        if (raw == null) return fallback;
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static BigDecimal parseDecimal(Object raw, BigDecimal fallback) {
        if (raw == null) return fallback;
        try {
            return new BigDecimal(String.valueOf(raw));
        } catch (Exception e) {
            return fallback;
        }
    }
}
