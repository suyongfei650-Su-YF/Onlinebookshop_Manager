package com.bookshop.admin.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.dto.AdminOrderDetail;
import com.bookshop.admin.dto.OrderAdminRow;
import com.bookshop.admin.dto.OrderStatusCount;
import com.bookshop.admin.dto.UserOrderItemRow;
import com.bookshop.admin.entity.ShopOrder;
import com.bookshop.admin.mapper.OrderItemMapper;
import com.bookshop.admin.mapper.ShopOrderMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminOrdersController {

    private final ShopOrderMapper shopOrderMapper;
    private final OrderItemMapper orderItemMapper;

    public AdminOrdersController(ShopOrderMapper shopOrderMapper, OrderItemMapper orderItemMapper) {
        this.shopOrderMapper = shopOrderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        try {
            size = Math.min(Math.max(size, 1), 100);
            page = Math.max(page, 1);
            String kw = keyword == null || keyword.trim().isEmpty() ? null : keyword.trim();
            String st = status == null || status.trim().isEmpty() ? null : status.trim();
            int offset = (page - 1) * size;
            long total = shopOrderMapper.countAdminOrders(kw, st);
            List<OrderAdminRow> list = shopOrderMapper.selectAdminOrderPage(kw, st, offset, size);
            Map<String, Long> statusCounts = toStatusCountMap(shopOrderMapper.countOrdersByStatus());
            BigDecimal paidTotal = shopOrderMapper.sumTotalAmount();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("list", list);
            data.put("total", total);
            data.put("page", page);
            data.put("size", size);
            data.put("statusCounts", statusCounts);
            data.put("paidTotalAmount", paidTotal);
            return ResponseEntity.ok(ApiResult.ok(data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResult<AdminOrderDetail>> detail(@PathVariable Long id) {
        try {
            OrderAdminRow row = shopOrderMapper.selectAdminOrderById(id);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("订单不存在"));
            }
            AdminOrderDetail detail = copyToDetail(row);
            List<UserOrderItemRow> items = orderItemMapper.selectItemsByOrderId(id);
            detail.setItems(items);
            return ResponseEntity.ok(ApiResult.ok(detail));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }

    @PostMapping("/orders/{id}/ship")
    public ResponseEntity<ApiResult<ShopOrder>> ship(@PathVariable Long id) {
        return updateStatus(id, "PENDING_SHIP", "SHIPPED", "发货");
    }

    @PostMapping("/orders/{id}/complete")
    public ResponseEntity<ApiResult<ShopOrder>> complete(@PathVariable Long id) {
        return updateStatus(id, "SHIPPED", "DONE", "完成");
    }

    private ResponseEntity<ApiResult<ShopOrder>> updateStatus(
            Long id, String expected, String next, String actionLabel) {
        try {
            ShopOrder order = shopOrderMapper.selectById(id);
            if (order == null) {
                return ResponseEntity.status(404).body(ApiResult.fail("订单不存在"));
            }
            if (!expected.equalsIgnoreCase(order.getStatus())) {
                return ResponseEntity.badRequest().body(ApiResult.fail("当前状态不可" + actionLabel));
            }
            shopOrderMapper.update(
                    null,
                    new LambdaUpdateWrapper<ShopOrder>()
                            .eq(ShopOrder::getId, id)
                            .set(ShopOrder::getStatus, next));
            order.setStatus(next);
            return ResponseEntity.ok(ApiResult.ok(order));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(actionLabel + "失败: " + e.getMessage()));
        }
    }

    private static Map<String, Long> toStatusCountMap(List<OrderStatusCount> rows) {
        Map<String, Long> map = new HashMap<>();
        if (rows != null) {
            for (OrderStatusCount row : rows) {
                if (row.getStatus() != null && row.getCount() != null) {
                    map.put(row.getStatus(), row.getCount());
                }
            }
        }
        return map;
    }

    private static AdminOrderDetail copyToDetail(OrderAdminRow row) {
        AdminOrderDetail detail = new AdminOrderDetail();
        detail.setId(row.getId());
        detail.setOrderNo(row.getOrderNo());
        detail.setCustomerId(row.getCustomerId());
        detail.setCustomerNickname(row.getCustomerNickname());
        detail.setCustomerUsername(row.getCustomerUsername());
        detail.setCustomerAvatarUrl(row.getCustomerAvatarUrl());
        detail.setTotalAmount(row.getTotalAmount());
        detail.setStatus(row.getStatus());
        detail.setReceiverName(row.getReceiverName());
        detail.setReceiverPhone(row.getReceiverPhone());
        detail.setShippingAddress(row.getShippingAddress());
        detail.setPaymentMethod(row.getPaymentMethod());
        detail.setItemCount(row.getItemCount());
        detail.setCreatedAt(row.getCreatedAt());
        return detail;
    }
}
