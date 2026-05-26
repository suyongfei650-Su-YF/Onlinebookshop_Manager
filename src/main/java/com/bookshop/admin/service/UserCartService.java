package com.bookshop.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookshop.admin.entity.Book;
import com.bookshop.admin.entity.CartItem;
import com.bookshop.admin.mapper.BookMapper;
import com.bookshop.admin.mapper.CartItemMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserCartService {

    private final CartItemMapper cartItemMapper;
    private final BookMapper bookMapper;

    public UserCartService(CartItemMapper cartItemMapper, BookMapper bookMapper) {
        this.cartItemMapper = cartItemMapper;
        this.bookMapper = bookMapper;
    }

    public static class AddResult {
        private final boolean success;
        private final String message;
        private final Long bookId;
        private final String bookTitle;
        private final int quantity;
        private final long cartCount;

        private AddResult(
                boolean success,
                String message,
                Long bookId,
                String bookTitle,
                int quantity,
                long cartCount) {
            this.success = success;
            this.message = message;
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.quantity = quantity;
            this.cartCount = cartCount;
        }

        public static AddResult fail(String message) {
            return new AddResult(false, message, null, null, 0, 0);
        }

        public static AddResult ok(String message, Long bookId, String bookTitle, int quantity, long cartCount) {
            return new AddResult(true, message, bookId, bookTitle, quantity, cartCount);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Long getBookId() {
            return bookId;
        }

        public String getBookTitle() {
            return bookTitle;
        }

        public int getQuantity() {
            return quantity;
        }

        public long getCartCount() {
            return cartCount;
        }
    }

    public AddResult addItem(Long userId, Long bookId, int addQty) {
        if (userId == null || userId <= 0) {
            return AddResult.fail("未登录");
        }
        if (bookId == null || bookId <= 0) {
            return AddResult.fail("无效的图书编号");
        }
        int qty = Math.max(1, addQty);

        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return AddResult.fail("图书不存在");
        }
        if (!"ON_SHELF".equalsIgnoreCase(book.getStatus())) {
            return AddResult.fail("该图书已下架，无法加入购物车");
        }
        if (book.getStock() == null || book.getStock() <= 0) {
            return AddResult.fail("该图书暂时缺货");
        }

        CartItem existing = cartItemMapper.selectOne(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getCustomerId, userId)
                        .eq(CartItem::getBookId, bookId));
        int finalQty;
        if (existing != null) {
            finalQty = Math.min(book.getStock(), existing.getQuantity() + qty);
            existing.setQuantity(finalQty);
            existing.setUpdatedAt(LocalDateTime.now());
            cartItemMapper.updateById(existing);
        } else {
            finalQty = Math.min(book.getStock(), qty);
            CartItem row = new CartItem();
            row.setCustomerId(userId);
            row.setBookId(bookId);
            row.setQuantity(finalQty);
            row.setCreatedAt(LocalDateTime.now());
            row.setUpdatedAt(LocalDateTime.now());
            cartItemMapper.insert(row);
        }

        return AddResult.ok(
                "已加入购物车",
                bookId,
                book.getTitle(),
                finalQty,
                countItems(userId));
    }

    public long countItems(Long userId) {
        if (userId == null) {
            return 0;
        }
        Long count = cartItemMapper.selectCount(
                new LambdaQueryWrapper<CartItem>().eq(CartItem::getCustomerId, userId));
        return count != null ? count : 0;
    }
}
