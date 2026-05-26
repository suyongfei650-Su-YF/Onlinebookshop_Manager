package com.bookshop.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookshop.admin.dto.GuestbookMessageRow;
import com.bookshop.admin.entity.GuestbookMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GuestbookMessageMapper extends BaseMapper<GuestbookMessage> {

    List<GuestbookMessageRow> selectVisiblePage(@Param("offset") int offset, @Param("limit") int limit);

    long countVisible();

    GuestbookMessageRow selectVisibleById(@Param("id") Long id);

    List<GuestbookMessageRow> selectAdminPage(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("keyword") String keyword,
            @Param("replyFilter") String replyFilter);

    long countAdmin(@Param("keyword") String keyword, @Param("replyFilter") String replyFilter);

    long countAdminPending();

    GuestbookMessageRow selectAdminById(@Param("id") Long id);
}
