package com.bookshop.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookshop.admin.entity.Book;
import com.bookshop.admin.dto.PortalBookDetail;
import com.bookshop.admin.dto.PortalCategoryRow;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BookMapper extends BaseMapper<Book> {

    long countByKeyword(@Param("kw") String kw, @Param("status") String status, @Param("categoryId") Long categoryId);

    List<Book> selectPageByKeyword(@Param("kw") String kw, @Param("status") String status, @Param("categoryId") Long categoryId, @Param("offset") int offset, @Param("limit") int limit);

    long countPortal(
            @Param("kw") String kw,
            @Param("categoryId") Long categoryId,
            @Param("priceMin") BigDecimal priceMin,
            @Param("priceMax") BigDecimal priceMax);

    List<Book> selectPortalPage(
            @Param("kw") String kw,
            @Param("categoryId") Long categoryId,
            @Param("priceMin") BigDecimal priceMin,
            @Param("priceMax") BigDecimal priceMax,
            @Param("sort") String sort,
            @Param("offset") int offset,
            @Param("limit") int limit);

    List<PortalCategoryRow> selectPortalCategoryStats();

    PortalBookDetail selectPortalBookDetail(@Param("id") Long id);

    List<com.bookshop.admin.dto.DashboardTopBookRow> selectStockAlertBooks(
            @Param("mode") String mode,
            @Param("threshold") int threshold,
            @Param("reduceMinStock") int reduceMinStock,
            @Param("reduceMaxSold") int reduceMaxSold,
            @Param("limit") int limit);
}
