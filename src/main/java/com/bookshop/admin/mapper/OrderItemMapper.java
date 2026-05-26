package com.bookshop.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookshop.admin.dto.BookAlertCategoryStat;
import com.bookshop.admin.dto.DashboardTopBookRow;
import com.bookshop.admin.dto.UserOrderItemRow;
import com.bookshop.admin.entity.OrderItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderItemMapper extends BaseMapper<OrderItem> {

    List<UserOrderItemRow> selectItemsByCustomerId(@Param("customerId") Long customerId);

    List<UserOrderItemRow> selectItemsByOrderId(@Param("orderId") Long orderId);

    List<DashboardTopBookRow> selectTopSellingBooks(@Param("limit") int limit);

    List<BookAlertCategoryStat> selectCategorySalesStats();

    List<DashboardTopBookRow> selectHotSellingRecent(@Param("days") int days, @Param("minSold") int minSold, @Param("limit") int limit);
}
