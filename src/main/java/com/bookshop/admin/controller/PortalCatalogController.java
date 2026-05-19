package com.bookshop.admin.controller;

import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.dto.PortalBookDetail;
import com.bookshop.admin.dto.PortalCategoryRow;
import com.bookshop.admin.entity.Book;
import com.bookshop.admin.mapper.BookMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
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

    private final BookMapper bookMapper;

    public PortalCatalogController(BookMapper bookMapper) {
        this.bookMapper = bookMapper;
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
}
