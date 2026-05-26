package com.bookshop.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("guestbook_message")
public class GuestbookMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String content;
    /** VISIBLE | HIDDEN */
    private String status;
    private String adminReply;
    private LocalDateTime adminReplyAt;
    private Long adminReplierId;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getAdminReply() {
        return adminReply;
    }

    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
    }

    public LocalDateTime getAdminReplyAt() {
        return adminReplyAt;
    }

    public void setAdminReplyAt(LocalDateTime adminReplyAt) {
        this.adminReplyAt = adminReplyAt;
    }

    public Long getAdminReplierId() {
        return adminReplierId;
    }

    public void setAdminReplierId(Long adminReplierId) {
        this.adminReplierId = adminReplierId;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
