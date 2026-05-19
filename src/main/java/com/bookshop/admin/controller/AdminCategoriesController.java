package com.bookshop.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.dto.AdminCategoryRow;
import com.bookshop.admin.entity.Book;
import com.bookshop.admin.entity.Category;
import com.bookshop.admin.mapper.BookMapper;
import com.bookshop.admin.mapper.CategoryMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminCategoriesController {

    private final CategoryMapper categoryMapper;
    private final BookMapper bookMapper;

    public AdminCategoriesController(CategoryMapper categoryMapper, BookMapper bookMapper) {
        this.categoryMapper = categoryMapper;
        this.bookMapper = bookMapper;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResult<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword) {
        try {
            List<Category> all = categoryMapper.selectList(
                    Wrappers.<Category>lambdaQuery()
                            .orderByAsc(Category::getSortWeight)
                            .orderByAsc(Category::getId));
            String kw = trimOrNull(keyword);
            List<AdminCategoryRow> rows = new ArrayList<>();
            for (Category c : all) {
                AdminCategoryRow row = toRow(c);
                row.setBookCount(countBooksInCategory(c.getId()));
                if (matchesKeyword(row, kw)) {
                    rows.add(row);
                }
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("list", rows);
            data.put("stats", buildStats(all));
            return ResponseEntity.ok(ApiResult.ok(data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResult<AdminCategoryRow>> create(@RequestBody(required = false) Map<String, Object> body) {
        try {
            if (body == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请求体不能为空"));
            }
            Category c = parseFromBody(body, null);
            if (c.getName() == null || c.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResult.fail("分类名称不能为空"));
            }
            if (c.getParentId() != null && categoryMapper.selectById(c.getParentId()) == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("父级分类不存在"));
            }
            if (c.getSortWeight() == null) {
                c.setSortWeight(0);
            }
            if (c.getVisible() == null) {
                c.setVisible(1);
            }
            categoryMapper.insert(c);
            AdminCategoryRow row = toRow(c);
            row.setBookCount(0);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(row));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("新增分类失败: " + e.getMessage()));
        }
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResult<AdminCategoryRow>> update(
            @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            Category old = categoryMapper.selectById(id);
            if (old == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("分类不存在"));
            }
            if (body == null) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请求体不能为空"));
            }
            Category next = parseFromBody(body, old);
            next.setId(id);
            if (id.equals(next.getParentId())) {
                return ResponseEntity.badRequest().body(ApiResult.fail("不能将分类设为自己的子级"));
            }
            if (next.getParentId() != null) {
                if (categoryMapper.selectById(next.getParentId()) == null) {
                    return ResponseEntity.badRequest().body(ApiResult.fail("父级分类不存在"));
                }
                if (isDescendant(id, next.getParentId())) {
                    return ResponseEntity.badRequest().body(ApiResult.fail("不能将子分类设为父级"));
                }
            }
            categoryMapper.updateById(next);
            AdminCategoryRow row = toRow(categoryMapper.selectById(id));
            row.setBookCount(countBooksInCategory(id));
            return ResponseEntity.ok(ApiResult.ok(row));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("更新分类失败: " + e.getMessage()));
        }
    }

    @PostMapping("/categories/{id}/toggle-visible")
    public ResponseEntity<ApiResult<AdminCategoryRow>> toggleVisible(@PathVariable Long id) {
        try {
            Category old = categoryMapper.selectById(id);
            if (old == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("分类不存在"));
            }
            old.setVisible(old.getVisible() != null && old.getVisible() == 1 ? 0 : 1);
            categoryMapper.updateById(old);
            AdminCategoryRow row = toRow(old);
            row.setBookCount(countBooksInCategory(id));
            return ResponseEntity.ok(ApiResult.ok(row));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("更新展示状态失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResult<Void>> delete(@PathVariable Long id) {
        try {
            Category old = categoryMapper.selectById(id);
            if (old == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("分类不存在"));
            }
            long childCount = categoryMapper.selectCount(
                    Wrappers.<Category>lambdaQuery().eq(Category::getParentId, id));
            if (childCount > 0) {
                return ResponseEntity.badRequest().body(ApiResult.fail("请先删除或移走该分类下的子分类"));
            }
            long bookCount = bookMapper.selectCount(
                    Wrappers.<Book>lambdaQuery().eq(Book::getCategoryId, id));
            if (bookCount > 0) {
                return ResponseEntity.badRequest().body(
                        ApiResult.fail("该分类下仍有 " + bookCount + " 本图书，请先在图书管理中调整分类"));
            }
            categoryMapper.deleteById(id);
            return ResponseEntity.ok(ApiResult.ok(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("删除分类失败: " + e.getMessage()));
        }
    }

    private Map<String, Object> buildStats(List<Category> all) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCategories", all.size());
        long visibleCount = all.stream().filter(c -> c.getVisible() != null && c.getVisible() == 1).count();
        stats.put("visibleCount", visibleCount);
        long categorized = bookMapper.selectCount(
                Wrappers.<Book>lambdaQuery().isNotNull(Book::getCategoryId));
        long uncategorized = bookMapper.selectCount(
                Wrappers.<Book>lambdaQuery().isNull(Book::getCategoryId));
        stats.put("categorizedBooks", categorized);
        stats.put("uncategorizedBooks", uncategorized);
        return stats;
    }

    private int countBooksInCategory(Long categoryId) {
        if (categoryId == null) {
            return 0;
        }
        Long n = bookMapper.selectCount(Wrappers.<Book>lambdaQuery().eq(Book::getCategoryId, categoryId));
        return n == null ? 0 : n.intValue();
    }

    private boolean isDescendant(Long ancestorId, Long candidateParentId) {
        Long current = candidateParentId;
        while (current != null) {
            if (ancestorId.equals(current)) {
                return true;
            }
            Category parent = categoryMapper.selectById(current);
            if (parent == null) {
                break;
            }
            current = parent.getParentId();
        }
        return false;
    }

    private static boolean matchesKeyword(AdminCategoryRow row, String kw) {
        if (kw == null) {
            return true;
        }
        String lower = kw.toLowerCase(Locale.ROOT);
        return contains(row.getName(), lower)
                || contains(row.getNameEn(), lower)
                || contains(row.getCode(), lower)
                || String.valueOf(row.getId()).contains(kw);
    }

    private static boolean contains(String s, String lower) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(lower);
    }

    private static AdminCategoryRow toRow(Category c) {
        AdminCategoryRow row = new AdminCategoryRow();
        row.setId(c.getId());
        row.setParentId(c.getParentId());
        row.setName(c.getName());
        row.setNameEn(c.getNameEn());
        row.setCode(c.getCode());
        row.setSortWeight(c.getSortWeight());
        row.setVisible(c.getVisible());
        row.setCreatedAt(c.getCreatedAt());
        return row;
    }

    private static Category parseFromBody(Map<String, Object> body, Category fallback) {
        Category c = new Category();
        c.setParentId(parseLong(body.get("parentId"), fallback == null ? null : fallback.getParentId()));
        c.setName(parseStr(body.get("name"), fallback == null ? null : fallback.getName()));
        c.setNameEn(parseStr(body.get("nameEn"), fallback == null ? null : fallback.getNameEn()));
        c.setCode(parseStr(body.get("code"), fallback == null ? null : fallback.getCode()));
        c.setSortWeight(parseInt(body.get("sortWeight"), fallback == null ? null : fallback.getSortWeight()));
        c.setVisible(parseInt(body.get("visible"), fallback == null ? null : fallback.getVisible()));
        return c;
    }

    private static String trimOrNull(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        return s.trim();
    }

    private static String parseStr(Object raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String s = String.valueOf(raw).trim();
        return s.isEmpty() ? fallback : s;
    }

    private static Long parseLong(Object raw, Long fallback) {
        if (raw == null || "".equals(String.valueOf(raw).trim())) {
            return fallback;
        }
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Integer parseInt(Object raw, Integer fallback) {
        if (raw == null || "".equals(String.valueOf(raw).trim())) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
