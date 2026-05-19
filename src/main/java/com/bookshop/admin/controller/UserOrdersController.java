package com.bookshop.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.dto.CartBookRow;
import com.bookshop.admin.dto.UserOrderItemRow;
import com.bookshop.admin.dto.UserOrderRow;
import com.bookshop.admin.entity.Book;
import com.bookshop.admin.entity.CartItem;
import com.bookshop.admin.entity.OrderItem;
import com.bookshop.admin.entity.ShopOrder;
import com.bookshop.admin.mapper.BookMapper;
import com.bookshop.admin.mapper.CartItemMapper;
import com.bookshop.admin.mapper.OrderItemMapper;
import com.bookshop.admin.mapper.ShopOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/orders")
public class UserOrdersController {

    private static final Logger log = LoggerFactory.getLogger(UserOrdersController.class);

    private final ShopOrderMapper shopOrderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final BookMapper bookMapper;

    public UserOrdersController(
            ShopOrderMapper shopOrderMapper,
            OrderItemMapper orderItemMapper,
            CartItemMapper cartItemMapper,
            BookMapper bookMapper) {
        this.shopOrderMapper = shopOrderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartItemMapper = cartItemMapper;
        this.bookMapper = bookMapper;
    }

    private static Long getUserId(HttpSession session) {
        Object idObj = session == null ? null : session.getAttribute("USER_ID");
        if (!(idObj instanceof Number)) {
            return null;
        }
        return ((Number) idObj).longValue();
    }

    private static UserOrderRow toRow(ShopOrder order) {
        UserOrderRow row = new UserOrderRow();
        row.setId(order.getId());
        row.setOrderNo(order.getOrderNo());
        row.setTotalAmount(order.getTotalAmount());
        row.setStatus(order.getStatus());
        row.setCreatedAt(order.getCreatedAt());
        return row;
    }

    private static List<Long> parseBookIds(Map<String, Object> body) {
        if (body == null) {
            return new ArrayList<>();
        }
        Object raw = body.get("bookIds");
        if (!(raw instanceof List)) {
            return new ArrayList<>();
        }
        List<Long> ids = new ArrayList<>();
        for (Object item : (List<?>) raw) {
            if (item instanceof Number) {
                long id = ((Number) item).longValue();
                if (id > 0) {
                    ids.add(id);
                }
            } else if (item != null) {
                try {
                    long id = Long.parseLong(String.valueOf(item).trim());
                    if (id > 0) {
                        ids.add(id);
                    }
                } catch (NumberFormatException ignored) {
                    /* skip */
                }
            }
        }
        return ids;
    }

    private static String parseStr(Map<String, Object> body, String key, int maxLen) {
        if (body == null) {
            return null;
        }
        Object raw = body.get(key);
        if (raw == null) {
            return null;
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            return null;
        }
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    private static boolean parsePayNow(Map<String, Object> body) {
        if (body == null) {
            return false;
        }
        Object raw = body.get("payNow");
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        if (raw != null) {
            return "true".equalsIgnoreCase(String.valueOf(raw).trim())
                    || "1".equals(String.valueOf(raw).trim());
        }
        return false;
    }

    private static String generateOrderNo() {
        String time = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "ORD-" + time + "-" + suffix;
    }

    private List<UserOrderRow> buildUserOrders(Long customerId) {
        List<ShopOrder> orders = shopOrderMapper.selectByCustomerId(customerId);
        if (orders == null || orders.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, UserOrderRow> map = new LinkedHashMap<>();
        for (ShopOrder order : orders) {
            map.put(order.getId(), toRow(order));
        }
        List<UserOrderItemRow> items = orderItemMapper.selectItemsByCustomerId(customerId);
        if (items != null) {
            for (UserOrderItemRow item : items) {
                UserOrderRow order = map.get(item.getOrderId());
                if (order != null) {
                    order.getItems().add(item);
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    @GetMapping
    public ApiResult<List<UserOrderRow>> list(HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) {
                return ApiResult.fail("未登录或会话已过期，请先登录");
            }
            return ApiResult.ok(buildUserOrders(userId));
        } catch (DataAccessException e) {
            log.error("list user orders failed", e);
            return ApiResult.fail("加载订单失败，请确认数据库 shop_order / order_item 表已创建");
        } catch (Exception e) {
            log.error("list user orders failed", e);
            return ApiResult.fail("加载订单失败，请稍后重试");
        }
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<UserOrderRow> create(@RequestBody(required = false) Map<String, Object> body, HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) {
                return ApiResult.fail("未登录或会话已过期，请先登录");
            }

            String receiverName = parseStr(body, "receiverName", 64);
            String receiverPhone = parseStr(body, "receiverPhone", 32);
            String shippingAddress = parseStr(body, "shippingAddress", 512);
            String paymentMethod = parseStr(body, "paymentMethod", 32);
            boolean payNow = parsePayNow(body);

            if (receiverName == null || receiverPhone == null || shippingAddress == null) {
                return ApiResult.fail("请填写完整的收货人、联系电话和收货地址");
            }
            if (paymentMethod == null) {
                return ApiResult.fail("请选择支付方式");
            }

            List<Long> bookIds = parseBookIds(body);
            List<CartBookRow> cartRows = cartItemMapper.selectByCustomerId(userId);
            if (cartRows == null || cartRows.isEmpty()) {
                return ApiResult.fail("购物车为空，请先添加商品");
            }

            List<CartBookRow> checkoutRows;
            if (bookIds.isEmpty()) {
                checkoutRows = cartRows.stream().filter(UserOrdersController::canCheckout).collect(Collectors.toList());
            } else {
                checkoutRows = cartRows.stream()
                        .filter(row -> bookIds.contains(row.getBookId()) && canCheckout(row))
                        .collect(Collectors.toList());
            }

            if (checkoutRows.isEmpty()) {
                return ApiResult.fail("没有可结算的商品，请检查库存或上架状态");
            }

            BigDecimal total = BigDecimal.ZERO;
            List<OrderItem> pendingItems = new ArrayList<>();

            for (CartBookRow row : checkoutRows) {
                Book book = bookMapper.selectById(row.getBookId());
                if (book == null) {
                    return ApiResult.fail("图书不存在：" + row.getBookId());
                }
                if (!"ON_SHELF".equalsIgnoreCase(book.getStatus())) {
                    return ApiResult.fail("《" + book.getTitle() + "》已下架，无法下单");
                }
                if (book.getStock() == null || book.getStock() < row.getQuantity()) {
                    return ApiResult.fail("《" + book.getTitle() + "》库存不足");
                }
                BigDecimal unitPrice = book.getPrice() != null ? book.getPrice() : BigDecimal.ZERO;
                BigDecimal line = unitPrice.multiply(BigDecimal.valueOf(row.getQuantity()));
                total = total.add(line);

                OrderItem item = new OrderItem();
                item.setBookId(book.getId());
                item.setQuantity(row.getQuantity());
                item.setUnitPrice(unitPrice);
                pendingItems.add(item);

                book.setStock(book.getStock() - row.getQuantity());
                bookMapper.updateById(book);
            }

            total = total.setScale(2, RoundingMode.HALF_UP);

            ShopOrder order = new ShopOrder();
            order.setOrderNo(generateOrderNo());
            order.setCustomerId(userId);
            order.setTotalAmount(total);
            order.setStatus(payNow ? "PENDING_SHIP" : "PENDING_PAY");
            order.setReceiverName(receiverName);
            order.setReceiverPhone(receiverPhone);
            order.setShippingAddress(shippingAddress);
            order.setPaymentMethod(paymentMethod);
            order.setCreatedAt(LocalDateTime.now());
            shopOrderMapper.insert(order);

            for (OrderItem item : pendingItems) {
                item.setOrderId(order.getId());
                orderItemMapper.insert(item);
            }

            for (CartBookRow row : checkoutRows) {
                cartItemMapper.delete(
                        new LambdaQueryWrapper<CartItem>()
                                .eq(CartItem::getCustomerId, userId)
                                .eq(CartItem::getBookId, row.getBookId()));
            }

            UserOrderRow result = toRow(order);
            List<UserOrderItemRow> itemRows = orderItemMapper.selectItemsByCustomerId(userId);
            if (itemRows != null) {
                for (UserOrderItemRow item : itemRows) {
                    if (order.getId().equals(item.getOrderId())) {
                        result.getItems().add(item);
                    }
                }
            }
            return ApiResult.ok(result);
        } catch (DataAccessException e) {
            log.error("create order failed", e);
            return ApiResult.fail("提交订单失败，请确认数据库表结构已更新");
        } catch (Exception e) {
            log.error("create order failed", e);
            return ApiResult.fail("提交订单失败，请稍后重试");
        }
    }

    private static boolean canCheckout(CartBookRow row) {
        return "ON_SHELF".equalsIgnoreCase(row.getStatus())
                && row.getStock() != null
                && row.getStock() > 0
                && row.getQuantity() != null
                && row.getQuantity() <= row.getStock();
    }

    @PostMapping("/{orderId}/pay")
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Map<String, Object>> pay(@PathVariable Long orderId, HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) {
                return ApiResult.fail("未登录或会话已过期，请先登录");
            }
            if (orderId == null || orderId <= 0) {
                return ApiResult.fail("无效的订单编号");
            }

            ShopOrder order = shopOrderMapper.selectById(orderId);
            if (order == null || !userId.equals(order.getCustomerId())) {
                return ApiResult.fail("订单不存在");
            }
            if (!"PENDING_PAY".equalsIgnoreCase(order.getStatus())) {
                return ApiResult.fail("当前订单状态不可支付");
            }

            shopOrderMapper.update(
                    null,
                    new LambdaUpdateWrapper<ShopOrder>()
                            .eq(ShopOrder::getId, orderId)
                            .eq(ShopOrder::getCustomerId, userId)
                            .set(ShopOrder::getStatus, "PENDING_SHIP"));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", "支付成功");
            data.put("status", "PENDING_SHIP");
            return ApiResult.ok(data);
        } catch (DataAccessException e) {
            log.error("pay order failed", e);
            return ApiResult.fail("支付失败，请稍后重试");
        } catch (Exception e) {
            log.error("pay order failed", e);
            return ApiResult.fail("支付失败，请稍后重试");
        }
    }

    @PostMapping("/{orderId}/cancel")
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Map<String, Object>> cancel(@PathVariable Long orderId, HttpSession session) {
        try {
            Long userId = getUserId(session);
            if (userId == null) {
                return ApiResult.fail("未登录或会话已过期，请先登录");
            }
            if (orderId == null || orderId <= 0) {
                return ApiResult.fail("无效的订单编号");
            }

            ShopOrder order = shopOrderMapper.selectById(orderId);
            if (order == null || !userId.equals(order.getCustomerId())) {
                return ApiResult.fail("订单不存在");
            }
            if (!"PENDING_PAY".equalsIgnoreCase(order.getStatus())) {
                return ApiResult.fail("仅待付款订单可取消");
            }

            List<UserOrderItemRow> items = orderItemMapper.selectItemsByCustomerId(userId);
            if (items != null) {
                for (UserOrderItemRow item : items) {
                    if (!orderId.equals(item.getOrderId())) {
                        continue;
                    }
                    Book book = bookMapper.selectById(item.getBookId());
                    if (book != null && book.getStock() != null) {
                        book.setStock(book.getStock() + item.getQuantity());
                        bookMapper.updateById(book);
                    }
                }
            }

            shopOrderMapper.update(
                    null,
                    new LambdaUpdateWrapper<ShopOrder>()
                            .eq(ShopOrder::getId, orderId)
                            .eq(ShopOrder::getCustomerId, userId)
                            .set(ShopOrder::getStatus, "CANCELLED"));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", "订单已取消");
            data.put("status", "CANCELLED");
            return ApiResult.ok(data);
        } catch (DataAccessException e) {
            log.error("cancel order failed", e);
            return ApiResult.fail("取消订单失败，请稍后重试");
        } catch (Exception e) {
            log.error("cancel order failed", e);
            return ApiResult.fail("取消订单失败，请稍后重试");
        }
    }
}
