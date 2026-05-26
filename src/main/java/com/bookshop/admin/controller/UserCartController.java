package com.bookshop.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.dto.CartBookRow;
import com.bookshop.admin.entity.Book;
import com.bookshop.admin.entity.CartItem;
import com.bookshop.admin.mapper.BookMapper;
import com.bookshop.admin.mapper.CartItemMapper;
import com.bookshop.admin.service.UserCartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/cart")
public class UserCartController {

    private static final Logger log = LoggerFactory.getLogger(UserCartController.class);

    private final CartItemMapper cartItemMapper;
    private final BookMapper bookMapper;
    private final UserCartService userCartService;

    public UserCartController(CartItemMapper cartItemMapper, BookMapper bookMapper, UserCartService userCartService) {
        this.cartItemMapper = cartItemMapper;
        this.bookMapper = bookMapper;
        this.userCartService = userCartService;
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

    private static int parseQuantity(Map<String, Object> body, int defaultQty) {
        if (body == null) {
            return defaultQty;
        }
        Object raw = body.get("quantity");
        if (raw instanceof Number) {
            return Math.max(1, ((Number) raw).intValue());
        }
        if (raw != null) {
            try {
                return Math.max(1, Integer.parseInt(String.valueOf(raw).trim()));
            } catch (NumberFormatException ignored) {
                return defaultQty;
            }
        }
        return defaultQty;
    }

    @GetMapping
    public ApiResult<List<CartBookRow>> list(HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) {
                return ApiResult.fail("未登录或会话已过期，请先登录");
            }
            return ApiResult.ok(cartItemMapper.selectByCustomerId(userId));
        } catch (DataAccessException e) {
            log.error("list cart failed", e);
            return ApiResult.fail("加载购物车失败");
        } catch (Exception e) {
            log.error("list cart failed", e);
            return ApiResult.fail("加载购物车失败，请稍后重试");
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
            int addQty = parseQuantity(body, 1);

            UserCartService.AddResult addResult = userCartService.addItem(userId, bookId, addQty);
            if (!addResult.isSuccess()) {
                return ApiResult.fail(addResult.getMessage());
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", addResult.getMessage());
            data.put("quantity", addResult.getQuantity());
            data.put("cartCount", addResult.getCartCount());
            return ApiResult.ok(data);
        } catch (DataAccessException e) {
            log.error("add cart failed", e);
            return ApiResult.fail("加入购物车失败");
        } catch (Exception e) {
            log.error("add cart failed", e);
            return ApiResult.fail("加入购物车失败，请稍后重试");
        }
    }

    @PutMapping("/{bookId}")
    public ApiResult<Map<String, Object>> updateQuantity(
            @PathVariable Long bookId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) {
                return ApiResult.fail("未登录或会话已过期，请先登录");
            }
            if (bookId == null || bookId <= 0) {
                return ApiResult.fail("无效的图书编号");
            }
            int qty = parseQuantity(body, 1);

            CartItem item = cartItemMapper.selectOne(
                    new LambdaQueryWrapper<CartItem>()
                            .eq(CartItem::getCustomerId, userId)
                            .eq(CartItem::getBookId, bookId));
            if (item == null) {
                return ApiResult.fail("购物车中无此商品");
            }

            Book book = bookMapper.selectById(bookId);
            if (book == null) {
                return ApiResult.fail("图书不存在");
            }

            if (qty <= 0) {
                cartItemMapper.deleteById(item.getId());
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("message", "已移除");
                data.put("cartCount", userCartService.countItems(userId));
                return ApiResult.ok(data);
            }

            int finalQty = Math.min(book.getStock() != null ? book.getStock() : qty, qty);
            item.setQuantity(finalQty);
            item.setUpdatedAt(LocalDateTime.now());
            cartItemMapper.updateById(item);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", "数量已更新");
            data.put("quantity", finalQty);
            return ApiResult.ok(data);
        } catch (DataAccessException e) {
            log.error("update cart failed", e);
            return ApiResult.fail("更新数量失败");
        } catch (Exception e) {
            log.error("update cart failed", e);
            return ApiResult.fail("更新数量失败，请稍后重试");
        }
    }

    @DeleteMapping("/{bookId}")
    public ApiResult<Map<String, Object>> remove(@PathVariable Long bookId, HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) {
                return ApiResult.fail("未登录或会话已过期，请先登录");
            }
            cartItemMapper.delete(
                    new LambdaQueryWrapper<CartItem>()
                            .eq(CartItem::getCustomerId, userId)
                            .eq(CartItem::getBookId, bookId));
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", "已从购物车移除");
            data.put("cartCount", userCartService.countItems(userId));
            return ApiResult.ok(data);
        } catch (DataAccessException e) {
            log.error("remove cart failed", e);
            return ApiResult.fail("删除失败");
        } catch (Exception e) {
            log.error("remove cart failed", e);
            return ApiResult.fail("删除失败，请稍后重试");
        }
    }

}
