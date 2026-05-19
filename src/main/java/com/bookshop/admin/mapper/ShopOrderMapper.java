package com.bookshop.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookshop.admin.dto.DashboardDayStat;
import com.bookshop.admin.dto.OrderAdminRow;
import com.bookshop.admin.dto.OrderStatusCount;
import com.bookshop.admin.entity.ShopOrder;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ShopOrderMapper extends BaseMapper<ShopOrder> {

    List<ShopOrder> selectByCustomerId(@Param("customerId") Long customerId);

    long countAdminOrders(@Param("keyword") String keyword, @Param("status") String status);

    List<OrderAdminRow> selectAdminOrderPage(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("size") int size);

    OrderAdminRow selectAdminOrderById(@Param("id") Long id);

    List<OrderStatusCount> countOrdersByStatus();

    BigDecimal sumTotalAmount();

    long countTodayOrders();

    BigDecimal sumTodayAmount();

    List<DashboardDayStat> selectSalesLast7Days();

    List<DashboardDayStat> selectOrderCountLast7Days();

    List<OrderAdminRow> selectRecentOrders(@Param("limit") int limit);
}
