package com.bookshop.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookshop.admin.entity.AdminUser;

import java.util.Optional;

public interface AdminUserMapper extends BaseMapper<AdminUser> {

    default Optional<AdminUser> findByUsername(String username) {
        return Optional.ofNullable(selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<AdminUser>lambdaQuery()
                        .eq(AdminUser::getUsername, username)));
    }
}
