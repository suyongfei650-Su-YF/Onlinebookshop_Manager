package com.bookshop.admin.controller;

import com.bookshop.admin.common.ApiResult;
import com.bookshop.admin.config.AiProperties;
import com.bookshop.admin.dto.PortalAiChatResult;
import com.bookshop.admin.service.ai.OpenAiCompatibleClient;
import com.bookshop.admin.service.ai.PortalAiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portal/ai")
public class PortalAiChatController {

    private final PortalAiChatService chatService;
    private final AiProperties aiProperties;

    public PortalAiChatController(PortalAiChatService chatService, AiProperties aiProperties) {
        this.chatService = chatService;
        this.aiProperties = aiProperties;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResult<Map<String, Object>>> status(HttpSession session) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", aiProperties.isEnabled());
        data.put("model", aiProperties.getModel());
        data.put("baseUrl", maskBaseUrl(aiProperties.getBaseUrl()));
        data.put("providerHint", "OpenAI 兼容 API（Ollama / DeepSeek / 通义等）");
        data.putAll(chatService.buildStatusExtras(session));
        return ResponseEntity.ok(ApiResult.ok(data));
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResult<PortalAiChatResult>> chat(
            @RequestBody(required = false) Map<String, Object> body, HttpSession session) {
        try {
            String message = body == null ? "" : String.valueOf(body.getOrDefault("message", "")).trim();
            List<OpenAiCompatibleClient.ChatMessage> history = parseHistory(body);
            PortalAiChatResult result = chatService.chat(message, history, session);
            return ResponseEntity.ok(ApiResult.ok(result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResult.fail("智能助手暂时不可用：" + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private static List<OpenAiCompatibleClient.ChatMessage> parseHistory(Map<String, Object> body) {
        List<OpenAiCompatibleClient.ChatMessage> out = new ArrayList<>();
        if (body == null) {
            return out;
        }
        Object raw = body.get("history");
        if (!(raw instanceof List)) {
            return out;
        }
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) item;
            String role = String.valueOf(m.getOrDefault("role", "user"));
            String content = String.valueOf(m.getOrDefault("content", "")).trim();
            if (content.isEmpty()) {
                continue;
            }
            out.add(new OpenAiCompatibleClient.ChatMessage(role, content));
        }
        return out;
    }

    private static String maskBaseUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        return url.trim().replaceAll("(?<=://)[^/@]+@", "***@");
    }
}
