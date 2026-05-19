package com.bookshop.admin.controller;

import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.service.BookCoverStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminBookCoverController {

    private final BookCoverStorageService bookCoverStorageService;

    public AdminBookCoverController(BookCoverStorageService bookCoverStorageService) {
        this.bookCoverStorageService = bookCoverStorageService;
    }

    @PostMapping("/books/cover-upload")
    public ResponseEntity<ApiResult<Map<String, String>>> upload(@RequestParam("file") MultipartFile file) {
        try {
            String coverUrl = bookCoverStorageService.save(file);
            Map<String, String> data = new LinkedHashMap<>();
            data.put("coverUrl", coverUrl);
            return ResponseEntity.ok(ApiResult.ok(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResult.fail(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("封面上传失败: " + e.getMessage()));
        }
    }
}
