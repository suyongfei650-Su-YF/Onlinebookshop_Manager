package com.bookshop.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookshop.admin.dto.AdminCustomerRow;
import com.bookshop.admin.entity.Customer;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CustomerMapper extends BaseMapper<Customer> {

    long countAdminCustomers(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("roleTag") String roleTag);

    List<AdminCustomerRow> selectAdminCustomerPage(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("roleTag") String roleTag,
            @Param("offset") int offset,
            @Param("size") int size);

    AdminCustomerRow selectAdminCustomerById(@Param("id") Long id);

    List<Map<String, Object>> countCustomersByStatus();

    long countNewCustomersToday();
}
