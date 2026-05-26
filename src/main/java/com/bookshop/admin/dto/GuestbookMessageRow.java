package com.bookshop.admin.dto;

import java.time.LocalDateTime;

/** 留言板列表项（含读者昵称） */
public class GuestbookMessageRow {

    private Long id;
    private Long customerId;
    private String content;
    private LocalDateTime createdAt;
    private String nickname;
    private String avatarUrl;
    private String status;
    private String adminReply;
    private LocalDateTime adminReplyAt;
    private String replierName;
    /** 当前登录用户是否为作者 */
    private Boolean mine;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Boolean getMine() {
        return mine;
    }

    public void setMine(Boolean mine) {
        this.mine = mine;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getReplierName() {
        return replierName;
    }

    public void setReplierName(String replierName) {
        this.replierName = replierName;
    }
}
