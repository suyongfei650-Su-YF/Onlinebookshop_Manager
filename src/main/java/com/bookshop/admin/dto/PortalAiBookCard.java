package com.bookshop.admin.dto;

import java.math.BigDecimal;

/** 门户 AI 回复中附带的图书卡片 */
public class PortalAiBookCard {
    private Long id;
    private String title;
    private String author;
    private BigDecimal price;
    private String coverUrl;
    private String categoryName;
    private Long soldQty;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
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
}
