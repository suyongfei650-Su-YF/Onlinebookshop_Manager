package com.bookshop.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.dto.FavoriteBookRow;
import com.bookshop.admin.entity.Book;
import com.bookshop.admin.entity.BookFavorite;
import com.bookshop.admin.mapper.BookFavoriteMapper;
import com.bookshop.admin.mapper.BookMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/favorites")
public class UserFavoritesController {

    private static final Logger log = LoggerFactory.getLogger(UserFavoritesController.class);

    private final BookFavoriteMapper bookFavoriteMapper;
    private final BookMapper bookMapper;

    public UserFavoritesController(BookFavoriteMapper bookFavoriteMapper, BookMapper bookMapper) {
        this.bookFavoriteMapper = bookFavoriteMapper;
        this.bookMapper = bookMapper;
    }

    private static Long getUserId(HttpSession session) {
        Object idObj = session == null ? null : session.getAttribute("USER_ID");
        if (!(idObj instanceof Number)) {
            return null;
        }
        return ((Number) idObj).longValue();
    }

    private static Long parseBookId(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object rawId = body.get("bookId");
        if (rawId instanceof Number) {
            return ((Number) rawId).longValue();
        }
        if (rawId != null) {
            try {
                return Long.parseLong(String.valueOf(rawId).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String dbHint(DataAccessException e) {
        String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        if (msg != null && msg.contains("book_favorite")) {
            return "收藏表未就绪，请重启后端或执行 sql/patch_book_favorite.sql";
        }
        return "数据库操作失败，请稍后重试";
    }

    @GetMapping
    public ApiResult<List<FavoriteBookRow>> list(HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) {
                return ApiResult.fail("未登录或会话已过期，请先登录");
            }
            return ApiResult.ok(bookFavoriteMapper.selectByCustomerId(userId));
        } catch (DataAccessException e) {
            log.error("list favorites failed", e);
            return ApiResult.fail(dbHint(e));
        } catch (Exception e) {
            log.error("list favorites failed", e);
            return ApiResult.fail("加载收藏失败，请稍后重试");
        }
    }

    @GetMapping("/check/{bookId}")
    public ApiResult<Map<String, Object>> check(@PathVariable Long bookId, HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) {
                return ApiResult.fail("未登录或会话已过期，请先登录");
            }
            if (bookId == null || bookId <= 0) {
                return ApiResult.fail("无效的图书编号");
            }
            long count = bookFavoriteMapper.countByCustomerAndBook(userId, bookId);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("favorited", count > 0);
            return ApiResult.ok(data);
        } catch (DataAccessException e) {
            log.error("check favorite failed", e);
            return ApiResult.fail(dbHint(e));
        } catch (Exception e) {
            log.error("check favorite failed", e);
            return ApiResult.fail("查询收藏状态失败");
        }
    }

    @PostMapping
    public ApiResult<Map<String, Object>> add(@RequestBody(required = false) Map<String, Object> body, HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) {
                return ApiResult.fail("未登录或会话已过期，请先登录");
            }
            Long bookId = parseBookId(body);
            if (bookId == null || bookId <= 0) {
                return ApiResult.fail("无效的图书编号");
            }

            Book book = bookMapper.selectById(bookId);
            if (book == null) {
                return ApiResult.fail("图书不存在");
            }

            long exists = bookFavoriteMapper.countByCustomerAndBook(userId, bookId);
            if (exists > 0) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("favorited", true);
                data.put("message", "已在收藏夹中");
                return ApiResult.ok(data);
            }

            BookFavorite row = new BookFavorite();
            row.setCustomerId(userId);
            row.setBookId(bookId);
            row.setCreatedAt(LocalDateTime.now());
            bookFavoriteMapper.insert(row);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("favorited", true);
            data.put("message", "收藏成功");
            return ApiResult.ok(data);
        } catch (DataAccessException e) {
            log.error("add favorite failed", e);
            return ApiResult.fail(dbHint(e));
        } catch (Exception e) {
            log.error("add favorite failed", e);
            return ApiResult.fail("收藏失败，请稍后重试");
        }
    }

    @DeleteMapping("/{bookId}")
    public ApiResult<Map<String, Object>> remove(@PathVariable Long bookId, HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) {
                return ApiResult.fail("未登录或会话已过期，请先登录");
            }
            if (bookId == null || bookId <= 0) {
                return ApiResult.fail("无效的图书编号");
            }

            bookFavoriteMapper.delete(
                    new LambdaQueryWrapper<BookFavorite>()
                            .eq(BookFavorite::getCustomerId, userId)
                            .eq(BookFavorite::getBookId, bookId));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("favorited", false);
            data.put("message", "已取消收藏");
            return ApiResult.ok(data);
        } catch (DataAccessException e) {
            log.error("remove favorite failed", e);
            return ApiResult.fail(dbHint(e));
        } catch (Exception e) {
            log.error("remove favorite failed", e);
            return ApiResult.fail("取消收藏失败，请稍后重试");
        }
    }
}
