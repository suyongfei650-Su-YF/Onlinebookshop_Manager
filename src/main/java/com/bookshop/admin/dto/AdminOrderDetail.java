package com.bookshop.admin.dto;

import java.util.ArrayList;
import java.util.List;

/** 管理员订单详情（含明细） */
public class AdminOrderDetail extends OrderAdminRow {
    private List<UserOrderItemRow> items = new ArrayList<>();

    public List<UserOrderItemRow> getItems() {
        return items;
    }

    public void setItems(List<UserOrderItemRow> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
