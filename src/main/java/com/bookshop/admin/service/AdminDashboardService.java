package com.bookshop.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bookshop.admin.dto.DashboardDayStat;
import com.bookshop.admin.entity.Book;
import com.bookshop.admin.entity.ShopOrder;
import com.bookshop.admin.mapper.BookMapper;
import com.bookshop.admin.mapper.CustomerMapper;
import com.bookshop.admin.mapper.OrderItemMapper;
import com.bookshop.admin.mapper.ShopOrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final BookMapper bookMapper;
    private final ShopOrderMapper shopOrderMapper;
    private final CustomerMapper customerMapper;
    private final OrderItemMapper orderItemMapper;

    public AdminDashboardService(
            BookMapper bookMapper,
            ShopOrderMapper shopOrderMapper,
            CustomerMapper customerMapper,
            OrderItemMapper orderItemMapper) {
        this.bookMapper = bookMapper;
        this.shopOrderMapper = shopOrderMapper;
        this.customerMapper = customerMapper;
        this.orderItemMapper = orderItemMapper;
    }

    public Map<String, Object> stats() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("totalBooks", bookMapper.selectCount(null));
        d.put("totalOrders", shopOrderMapper.selectCount(null));
        d.put("totalCustomers", customerMapper.selectCount(null));
        d.put("lowStockCount", bookMapper.selectCount(Wrappers.<Book>lambdaQuery()
                .eq(Book::getStatus, "ON_SHELF").lt(Book::getStock, 10)));
        d.put("pendingShipCount", shopOrderMapper.selectCount(Wrappers.<ShopOrder>lambdaQuery()
                .eq(ShopOrder::getStatus, "PENDING_SHIP")));
        d.put("pendingPayCount", shopOrderMapper.selectCount(Wrappers.<ShopOrder>lambdaQuery()
                .eq(ShopOrder::getStatus, "PENDING_PAY")));
        d.put("orderAmountSum", shopOrderMapper.sumTotalAmount().doubleValue());

        d.put("todayOrders", shopOrderMapper.countTodayOrders());
        d.put("todayAmount", shopOrderMapper.sumTodayAmount().doubleValue());
        d.put("newCustomersToday", customerMapper.countNewCustomersToday());

        d.put("salesLast7Days", buildLast7DaysSeries(shopOrderMapper.selectSalesLast7Days(), true));
        d.put("orderTrend7Days", buildLast7DaysSeries(shopOrderMapper.selectOrderCountLast7Days(), false));
        d.put("topBooks", orderItemMapper.selectTopSellingBooks(5));
        d.put("recentOrders", shopOrderMapper.selectRecentOrders(5));
        return d;
    }

    private static List<Map<String, Object>> buildLast7DaysSeries(List<DashboardDayStat> rows, boolean includeAmount) {
        Map<String, DashboardDayStat> byDay = new LinkedHashMap<>();
        if (rows != null) {
            for (DashboardDayStat row : rows) {
                if (row.getDay() != null) {
                    byDay.put(row.getDay(), row);
                }
            }
        }
        List<Map<String, Object>> series = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            String day = today.minusDays(i).format(DAY_FMT);
            DashboardDayStat stat = byDay.get(day);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("day", day);
            point.put("orderCount", stat != null && stat.getOrderCount() != null ? stat.getOrderCount() : 0L);
            if (includeAmount) {
                BigDecimal amt = stat != null && stat.getAmount() != null ? stat.getAmount() : BigDecimal.ZERO;
                point.put("amount", amt.doubleValue());
            }
            series.add(point);
        }
        return series;
    }
}
