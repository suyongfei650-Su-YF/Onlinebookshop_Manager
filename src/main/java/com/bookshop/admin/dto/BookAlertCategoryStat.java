package com.bookshop.admin.dto;

public class BookAlertCategoryStat {
    private Long categoryId;
    private String categoryName;
    private Long soldQty;
    private Long bookCount;
    private Long stockTotal;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getSoldQty() {
        return soldQty;
    }

    public void setSoldQty(Long soldQty) {
        this.soldQty = soldQty;
    }

    public Long getBookCount() {
        return bookCount;
    }

    public void setBookCount(Long bookCount) {
        this.bookCount = bookCount;
    }

    public Long getStockTotal() {
        return stockTotal;
    }

    public void setStockTotal(Long stockTotal) {
        this.stockTotal = stockTotal;
    }
}
