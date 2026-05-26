package com.bookshop.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 门户 AI 助手：对接 OpenAI 兼容接口（Ollama / DeepSeek / 通义等）。
 * 配置示例见 application.yml 中 app.ai.*
 */
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** 是否启用大模型（false 时仅返回馆藏检索结果与规则回复） */
    private boolean enabled = false;

    /** API 根地址，如 http://127.0.0.1:11434/v1 或 https://api.deepseek.com/v1 */
    private String baseUrl = "http://127.0.0.1:11434/v1";

    /** API Key（Ollama 本地可留空） */
    private String apiKey = "";

    /** 模型名称 */
    private String model = "llama3.2";

    private int connectTimeoutMs = 15_000;
    private int readTimeoutMs = 90_000;
    private int maxTokens = 1200;
    private double temperature = 0.6;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}
