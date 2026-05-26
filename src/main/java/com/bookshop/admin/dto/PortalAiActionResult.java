package com.bookshop.admin.dto;

/**
 * 智能助手触发的可执行动作结果（加购、查看购物车、需登录等）。
 */
public class PortalAiActionResult {

    /** LOGIN_REQUIRED | ADD_TO_CART | VIEW_CART */
    private String type;
    private boolean success;
    private String message;
    private Long bookId;
    private String bookTitle;
    private Long cartCount;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public Long getCartCount() {
        return cartCount;
    }

    public void setCartCount(Long cartCount) {
        this.cartCount = cartCount;
    }
}
