package com.bookshop.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookshop.admin.dto.CartBookRow;
import com.bookshop.admin.entity.CartItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CartItemMapper extends BaseMapper<CartItem> {

    List<CartBookRow> selectByCustomerId(@Param("customerId") Long customerId);
}
