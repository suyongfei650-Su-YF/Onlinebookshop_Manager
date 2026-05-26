package com.bookshop.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bookshop.admin.dto.BookAlertCategoryStat;
import com.bookshop.admin.dto.DashboardTopBookRow;
import com.bookshop.admin.entity.Book;
import com.bookshop.admin.mapper.BookMapper;
import com.bookshop.admin.mapper.OrderItemMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminBookAlertService {

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int REDUCE_MIN_STOCK = 15;
    private static final int REDUCE_MAX_SOLD = 5;
    private static final int HOT_DAYS = 30;
    private static final int HOT_MIN_SOLD = 3;
    private static final int LIST_LIMIT = 100;

    private final BookMapper bookMapper;
    private final OrderItemMapper orderItemMapper;

    public AdminBookAlertService(BookMapper bookMapper, OrderItemMapper orderItemMapper) {
        this.bookMapper = bookMapper;
        this.orderItemMapper = orderItemMapper;
    }

    public Map<String, Object> buildReport() {
        List<DashboardTopBookRow> outOfStock =
                bookMapper.selectStockAlertBooks("OUT", LOW_STOCK_THRESHOLD, REDUCE_MIN_STOCK, REDUCE_MAX_SOLD, LIST_LIMIT);
        List<DashboardTopBookRow> lowStock =
                bookMapper.selectStockAlertBooks("LOW", LOW_STOCK_THRESHOLD, REDUCE_MIN_STOCK, REDUCE_MAX_SOLD, LIST_LIMIT);
        List<DashboardTopBookRow> reduceCandidates =
                bookMapper.selectStockAlertBooks("REDUCE", LOW_STOCK_THRESHOLD, REDUCE_MIN_STOCK, REDUCE_MAX_SOLD, LIST_LIMIT);
        List<DashboardTopBookRow> hotSelling =
                orderItemMapper.selectHotSellingRecent(HOT_DAYS, HOT_MIN_SOLD, LIST_LIMIT);
        if (hotSelling.isEmpty()) {
            hotSelling = orderItemMapper.selectTopSellingBooks(LIST_LIMIT);
        }
        List<BookAlertCategoryStat> categoryStats = orderItemMapper.selectCategorySalesStats();
        List<BookAlertCategoryStat> suggestAddCategories = pickSuggestAddCategories(categoryStats);

        long outCount = bookMapper.selectCount(Wrappers.<Book>lambdaQuery()
                .eq(Book::getStatus, "ON_SHELF").le(Book::getStock, 0));
        long lowCount = bookMapper.selectCount(Wrappers.<Book>lambdaQuery()
                .eq(Book::getStatus, "ON_SHELF").gt(Book::getStock, 0).lt(Book::getStock, LOW_STOCK_THRESHOLD));
        long sufficientCount = bookMapper.selectCount(Wrappers.<Book>lambdaQuery()
                .eq(Book::getStatus, "ON_SHELF").ge(Book::getStock, LOW_STOCK_THRESHOLD));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        data.put("dateLabel", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日")));
        data.put("lowStockThreshold", LOW_STOCK_THRESHOLD);
        data.put("outOfStock", outOfStock);
        data.put("lowStock", lowStock);
        data.put("hotSelling", hotSelling);
        data.put("reduceCandidates", reduceCandidates);
        data.put("suggestAddCategories", suggestAddCategories);
        data.put("categoryStats", categoryStats);
        data.put("counts", buildCounts(outCount, lowCount, sufficientCount, outOfStock, lowStock, hotSelling, reduceCandidates));
        data.put("stockStatusPie", buildStockStatusPie(outCount, lowCount, sufficientCount));
        data.put("alertTypePie", buildAlertTypePie(outCount, lowCount, hotSelling.size(), reduceCandidates.size()));
        data.put("categorySalesPie", buildCategorySalesPie(categoryStats));
        data.put("categoryBarChart", buildCategoryBarChart(categoryStats));
        return data;
    }

    private static List<BookAlertCategoryStat> pickSuggestAddCategories(List<BookAlertCategoryStat> stats) {
        List<BookAlertCategoryStat> out = new ArrayList<>();
        if (stats == null) {
            return out;
        }
        for (BookAlertCategoryStat row : stats) {
            long sold = row.getSoldQty() != null ? row.getSoldQty() : 0L;
            long books = row.getBookCount() != null ? row.getBookCount() : 0L;
            if (sold >= 8 && books < 5) {
                out.add(row);
            } else if (books == 0 && sold == 0) {
                continue;
            } else if (books <= 2 && sold >= 3) {
                out.add(row);
            }
            if (out.size() >= 8) {
                break;
            }
        }
        return out;
    }

    private static Map<String, Long> buildCounts(
            long outCount,
            long lowCount,
            long sufficientCount,
            List<DashboardTopBookRow> outOfStock,
            List<DashboardTopBookRow> lowStock,
            List<DashboardTopBookRow> hotSelling,
            List<DashboardTopBookRow> reduceCandidates) {
        Map<String, Long> c = new LinkedHashMap<>();
        c.put("outOfStock", outCount);
        c.put("lowStock", lowCount);
        c.put("sufficient", sufficientCount);
        c.put("hotSelling", (long) hotSelling.size());
        c.put("reduceCandidates", (long) reduceCandidates.size());
        c.put("alertTotal", outCount + lowCount);
        return c;
    }

    private static List<Map<String, Object>> buildStockStatusPie(long outCount, long lowCount, long sufficientCount) {
        List<Map<String, Object>> pie = new ArrayList<>();
        pie.add(pieSlice("售空", outCount));
        pie.add(pieSlice("库存偏低", lowCount));
        pie.add(pieSlice("库存充足", sufficientCount));
        return pie;
    }

    private static List<Map<String, Object>> buildAlertTypePie(
            long outCount, long lowCount, int hotSize, int reduceSize) {
        List<Map<String, Object>> pie = new ArrayList<>();
        pie.add(pieSlice("售空预警", outCount));
        pie.add(pieSlice("低库存", lowCount));
        pie.add(pieSlice("畅销大卖", hotSize));
        pie.add(pieSlice("建议减量", reduceSize));
        return pie;
    }

    private static List<Map<String, Object>> buildCategorySalesPie(List<BookAlertCategoryStat> stats) {
        List<Map<String, Object>> pie = new ArrayList<>();
        if (stats == null || stats.isEmpty()) {
            pie.add(pieSlice("暂无销量", 1L));
            return pie;
        }
        long other = 0L;
        int top = 0;
        for (BookAlertCategoryStat row : stats) {
            long v = row.getSoldQty() != null ? row.getSoldQty() : 0L;
            if (v <= 0) {
                continue;
            }
            if (top < 6) {
                pie.add(pieSlice(row.getCategoryName(), v));
                top++;
            } else {
                other += v;
            }
        }
        if (other > 0) {
            pie.add(pieSlice("其他分类", other));
        }
        if (pie.isEmpty()) {
            pie.add(pieSlice("暂无销量", 1L));
        }
        return pie;
    }

    private static List<Map<String, Object>> buildCategoryBarChart(List<BookAlertCategoryStat> stats) {
        List<Map<String, Object>> bars = new ArrayList<>();
        if (stats == null) {
            return bars;
        }
        int n = 0;
        for (BookAlertCategoryStat row : stats) {
            if (n >= 8) {
                break;
            }
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("categoryName", row.getCategoryName());
            point.put("soldQty", row.getSoldQty() != null ? row.getSoldQty() : 0L);
            point.put("bookCount", row.getBookCount() != null ? row.getBookCount() : 0L);
            point.put("stockTotal", row.getStockTotal() != null ? row.getStockTotal() : 0L);
            bars.add(point);
            n++;
        }
        return bars;
    }

    private static Map<String, Object> pieSlice(String name, long value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }
}
