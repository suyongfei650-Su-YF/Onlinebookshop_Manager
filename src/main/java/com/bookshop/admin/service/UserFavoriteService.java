package com.bookshop.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookshop.admin.dto.FavoriteBookRow;
import com.bookshop.admin.entity.Book;
import com.bookshop.admin.entity.BookFavorite;
import com.bookshop.admin.mapper.BookFavoriteMapper;
import com.bookshop.admin.mapper.BookMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class UserFavoriteService {

    private final BookFavoriteMapper bookFavoriteMapper;
    private final BookMapper bookMapper;

    public UserFavoriteService(BookFavoriteMapper bookFavoriteMapper, BookMapper bookMapper) {
        this.bookFavoriteMapper = bookFavoriteMapper;
        this.bookMapper = bookMapper;
    }

    public static class FavoriteMutationResult {
        private final boolean success;
        private final String message;
        private final Long bookId;
        private final String bookTitle;
        private final boolean favorited;
        private final long favoriteCount;

        private FavoriteMutationResult(
                boolean success,
                String message,
                Long bookId,
                String bookTitle,
                boolean favorited,
                long favoriteCount) {
            this.success = success;
            this.message = message;
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.favorited = favorited;
            this.favoriteCount = favoriteCount;
        }

        public static FavoriteMutationResult fail(String message) {
            return new FavoriteMutationResult(false, message, null, null, false, 0);
        }

        public static FavoriteMutationResult ok(
                String message, Long bookId, String bookTitle, boolean favorited, long favoriteCount) {
            return new FavoriteMutationResult(true, message, bookId, bookTitle, favorited, favoriteCount);
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

        public boolean isFavorited() {
            return favorited;
        }

        public long getFavoriteCount() {
            return favoriteCount;
        }
    }

    public List<FavoriteBookRow> listByCustomer(Long customerId) {
        if (customerId == null || customerId <= 0) {
            return Collections.emptyList();
        }
        try {
            List<FavoriteBookRow> rows = bookFavoriteMapper.selectByCustomerId(customerId);
            return rows != null ? rows : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public long countByCustomer(Long customerId) {
        if (customerId == null || customerId <= 0) {
            return 0;
        }
        return listByCustomer(customerId).size();
    }

    public FavoriteMutationResult add(Long customerId, Long bookId) {
        if (customerId == null || customerId <= 0) {
            return FavoriteMutationResult.fail("未登录");
        }
        if (bookId == null || bookId <= 0) {
            return FavoriteMutationResult.fail("无效的图书编号");
        }

        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            return FavoriteMutationResult.fail("图书不存在");
        }

        long exists = bookFavoriteMapper.countByCustomerAndBook(customerId, bookId);
        if (exists > 0) {
            return FavoriteMutationResult.ok(
                    "已在收藏夹中", bookId, book.getTitle(), true, countByCustomer(customerId));
        }

        BookFavorite row = new BookFavorite();
        row.setCustomerId(customerId);
        row.setBookId(bookId);
        row.setCreatedAt(LocalDateTime.now());
        bookFavoriteMapper.insert(row);

        return FavoriteMutationResult.ok(
                "收藏成功", bookId, book.getTitle(), true, countByCustomer(customerId));
    }

    public FavoriteMutationResult remove(Long customerId, Long bookId) {
        if (customerId == null || customerId <= 0) {
            return FavoriteMutationResult.fail("未登录");
        }
        if (bookId == null || bookId <= 0) {
            return FavoriteMutationResult.fail("无效的图书编号");
        }

        Book book = bookMapper.selectById(bookId);
        String title = book != null ? book.getTitle() : null;

        bookFavoriteMapper.delete(
                new LambdaQueryWrapper<BookFavorite>()
                        .eq(BookFavorite::getCustomerId, customerId)
                        .eq(BookFavorite::getBookId, bookId));

        return FavoriteMutationResult.ok(
                "已取消收藏", bookId, title, false, countByCustomer(customerId));
    }
}
