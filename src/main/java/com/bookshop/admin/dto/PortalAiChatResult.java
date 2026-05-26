package com.bookshop.admin.dto;

import java.util.ArrayList;
import java.util.List;

public class PortalAiChatResult {
    private String reply;
    private List<PortalAiBookCard> books = new ArrayList<>();
    /** 是否调用了外部大模型 */
    private boolean usedAi;
    /** 未启用 AI 或调用失败时的说明 */
    private String hint;
    /** 当前请求是否已登录读者账号 */
    private boolean loggedIn;
    /** 未登录时为 true，与 loggedIn 互补 */
    private boolean guest;
    private String userNickname;
    /** 登录用户购物车品种数 */
    private Long cartCount;
    /** 登录用户收藏册数 */
    private Long favoriteCount;
    /** 当前用户已收藏图书 ID（供前端标记） */
    private List<Long> favoriteBookIds = new ArrayList<>();
    private List<PortalAiActionResult> actions = new ArrayList<>();

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public List<PortalAiBookCard> getBooks() {
        return books;
    }

    public void setBooks(List<PortalAiBookCard> books) {
        this.books = books;
    }

    public boolean isUsedAi() {
        return usedAi;
    }

    public void setUsedAi(boolean usedAi) {
        this.usedAi = usedAi;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public boolean isGuest() {
        return guest;
    }

    public void setGuest(boolean guest) {
        this.guest = guest;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public Long getCartCount() {
        return cartCount;
    }

    public void setCartCount(Long cartCount) {
        this.cartCount = cartCount;
    }

    public List<PortalAiActionResult> getActions() {
        return actions;
    }

    public void setActions(List<PortalAiActionResult> actions) {
        this.actions = actions;
    }

    public Long getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(Long favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public List<Long> getFavoriteBookIds() {
        return favoriteBookIds;
    }

    public void setFavoriteBookIds(List<Long> favoriteBookIds) {
        this.favoriteBookIds = favoriteBookIds;
    }
}
