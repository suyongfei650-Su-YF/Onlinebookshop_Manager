package com.bookshop.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookshop.admin.dto.FavoriteBookRow;
import com.bookshop.admin.entity.BookFavorite;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BookFavoriteMapper extends BaseMapper<BookFavorite> {

    List<FavoriteBookRow> selectByCustomerId(@Param("customerId") Long customerId);

    Long countByCustomerAndBook(@Param("customerId") Long customerId, @Param("bookId") Long bookId);
}
