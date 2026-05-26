# 门户智能阅读助手配置说明

门户右下角「智能阅读助手」支持：

- 按关键词在馆藏中检索图书并展示卡片
- 结合销量数据做推荐
- 对接 **OpenAI 兼容 API** 的大模型回答（找书、推荐、购书流程等）

未启用大模型时，仍可使用馆藏检索与规则回复。

### 游客与登录用户

| 能力 | 游客 | 已登录读者 |
|------|------|------------|
| 找书、推荐、购书说明 | ✓ | ✓ |
| 对话中加购 / 查看购物车 | 提示登录 | ✓（写入当前账号购物车） |
| 图书卡片「加购」按钮 | 引导登录 | 直接加购 |

示例话术：`把第一本加入购物车`、`查看我的购物车`、`把《书名》加入购物车`。

### 个人收藏

| 能力 | 游客 | 已登录读者 |
|------|------|------------|
| 查看我的收藏 | 提示登录 | 列出收藏书目卡片 |
| 根据收藏推荐 | 提示登录 | 按收藏分类/作者相似检索馆藏 |
| 对话收藏 / 取消收藏 | 提示登录 | 如「收藏第一本」「取消收藏第一本」 |
| 卡片「收藏」按钮 | 引导登录 | 一键加入/取消收藏 |

示例话术：`查看我的收藏`、`根据收藏推荐`、`收藏第一本`。

---

## 一、Ollama（本地开源，推荐答辩演示）

1. 安装 [Ollama](https://ollama.com/) 并执行：

   ```bash
   ollama pull llama3.2
   ```

2. 在 `src/main/resources/application-local.yml` 中增加：

   ```yaml
   app:
     ai:
       enabled: true
       base-url: http://127.0.0.1:11434/v1
       api-key:
       model: llama3.2
   ```

3. 重启 **启动后端 SpringBoot**，刷新门户页，助手标题应显示「大模型已连接」。

---

## 二、DeepSeek / 通义等云端 API

```yaml
app:
  ai:
    enabled: true
    base-url: https://api.deepseek.com/v1
    api-key: sk-你的密钥
    model: deepseek-chat
```

也可用环境变量：`AI_BASE_URL`、`AI_API_KEY`、`AI_MODEL`。

---

## 三、接口说明

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/portal/ai/status` | 是否启用 AI、模型名 |
| POST | `/api/portal/ai/chat` | 对话，body: `{ "message": "...", "history": [...] }` |

---

## 四、答辩说明要点

- 前端：浮动对话窗 + 图书卡片跳转详情
- 后端：`PortalAiChatService` 检索馆藏 → 组装上下文 → `OpenAiCompatibleClient` 调用兼容 API
- 可替换任意开源/商用大模型，仅需 OpenAI 格式 `POST /v1/chat/completions`
