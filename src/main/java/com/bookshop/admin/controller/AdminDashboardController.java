package com.bookshop.admin.controller;

import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResult<Map<String, Object>>> dashboard() {
        try {
            return ResponseEntity.ok(ApiResult.ok(adminDashboardService.stats()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }
}
