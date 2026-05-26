package com.bookshop.admin.controller;

import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.service.AdminBookAlertAiService;
import com.bookshop.admin.service.AdminBookAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminBookAlertController {

    private final AdminBookAlertService alertService;
    private final AdminBookAlertAiService alertAiService;

    public AdminBookAlertController(AdminBookAlertService alertService, AdminBookAlertAiService alertAiService) {
        this.alertService = alertService;
        this.alertAiService = alertAiService;
    }

    @GetMapping("/book-alerts")
    public ResponseEntity<ApiResult<Map<String, Object>>> report() {
        try {
            Map<String, Object> data = alertService.buildReport();
            data.put("ruleAnalysis", alertAiService.analyzeRuleOnly(data));
            return ResponseEntity.ok(ApiResult.ok(data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }

    @PostMapping("/book-alerts/ai-analyze")
    public ResponseEntity<ApiResult<Map<String, Object>>> aiAnalyze() {
        try {
            Map<String, Object> report = alertService.buildReport();
            Map<String, Object> ai = alertAiService.analyze(report);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("report", report);
            data.put("aiEnabled", ai.get("aiEnabled"));
            data.put("model", ai.get("model"));
            data.put("analysis", ai.get("analysis"));
            data.put("hint", ai.get("hint"));
            return ResponseEntity.ok(ApiResult.ok(data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail(e.getMessage()));
        }
    }
}
