# DeepSeek API 接入步骤（馆藏书屋门户 AI 助手）

本项目已支持 **OpenAI 兼容** 接口，DeepSeek 可直接使用，无需改 Java 代码。

---

## 第一步：注册并获取 API Key

1. 打开 DeepSeek 开放平台：https://platform.deepseek.com/
2. 注册 / 登录账号
3. 进入 **API Keys** 页面，点击 **创建 API Key**
4. 复制生成的密钥（形如 `sk-xxxxxxxx`，只显示一次请妥善保存）
5. 确认账户有余额（新用户一般有试用额度）

---

## 第二步：在项目中配置（推荐方式）

### 2.1 创建本地配置文件

在项目目录：

`src/main/resources/application-local.yml`

若不存在，可复制：

`src/main/resources/application-local.yml.example`

改名为 `application-local.yml`。

### 2.2 写入 DeepSeek 配置

在 `application-local.yml` 中加入（保留你原有的数据库配置）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/bookshop_admin?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true
    username: root
    password: 你的数据库密码

app:
  ai:
    enabled: true
    base-url: https://api.deepseek.com/v1
    api-key: sk-这里替换成你的DeepSeek密钥
    model: deepseek-chat
```

说明：

| 配置项 | 值 | 含义 |
|--------|-----|------|
| `enabled` | `true` | 开启大模型 |
| `base-url` | `https://api.deepseek.com/v1` | DeepSeek 官方地址（末尾要有 `/v1`） |
| `api-key` | `sk-...` | 你的 API Key |
| `model` | `deepseek-chat` | 对话模型（也可用 `deepseek-reasoner` 等，以平台文档为准） |

> `application-local.yml` 已在 `.gitignore` 中，**不要把 Key 提交到 Git**。

---

## 第三步：重启后端

1. 停止当前 **「启动后端 SpringBoot」**
2. 重新运行 **「启动后端 SpringBoot」**
3. 控制台无报错、出现 `Started BookshopAdminApplication` 即表示启动成功

---

## 第四步：验证是否接通

### 方式 A：浏览器

访问（Spring Boot dev 模式下）：

```
http://127.0.0.1:8080/Onlinebookshop_Manager/api/portal/ai/status
```

应看到 JSON，其中 `"enabled": true`，`"model": "deepseek-chat"`。

### 方式 B：门户页面

1. 打开 `http://127.0.0.1:5173/portal`
2. 点击右下角 **智能阅读助手**
3. 标题栏应显示：**大模型已连接 · deepseek-chat**
4. 输入「推荐几本文学类的书」测试回复

---

## 可选：用 IDEA 环境变量配置（不写 Key 进文件）

**Run → Edit Configurations → 启动后端 SpringBoot → Environment variables** 添加：

```
AI_BASE_URL=https://api.deepseek.com/v1
AI_API_KEY=sk-你的密钥
AI_MODEL=deepseek-chat
```

并在 `application-local.yml` 中只写：

```yaml
app:
  ai:
    enabled: true
```

（`application.yml` 里已支持 `${AI_BASE_URL}` 等占位符。）

---

## 常见问题

### 1. 仍显示「馆藏检索模式」

- 检查 `app.ai.enabled` 是否为 `true`
- 是否重启了 Spring Boot
- `application-local.yml` 是否在 `src/main/resources/` 下且文件名正确

### 2. 402 Payment Required / Insufficient Balance（余额不足）

- 表示 **DeepSeek 账户余额已用完**，与项目代码无关
- 登录 https://platform.deepseek.com → **充值** 或领取活动额度
- 充值前助手仍可使用 **本地操作说明**（如「如何搜索和购买」），找书检索不受影响
- 也可改用本地 **Ollama**（免费），见 `scripts/门户AI助手配置说明.md`

### 3. 提示大模型不可用 / 401

- API Key 是否完整、无多余空格
- Key 是否有效、账户是否有余额
- `base-url` 是否为 `https://api.deepseek.com/v1`

### 4. 超时

在 `application-local.yml` 增大超时：

```yaml
app:
  ai:
    read-timeout-ms: 120000
```

### 5. 本机无法访问外网

需能访问 `api.deepseek.com`；校园网可尝试手机热点。

---

## 费用与安全

- DeepSeek 按 token 计费，请在平台查看用量
- **切勿** 将 `sk-` 密钥写进代码仓库或发给他人
- 答辩演示用完可在平台禁用或删除 Key

---

## 对应代码位置（供说明书引用）

- 配置类：`com.bookshop.admin.config.AiProperties`
- 调用客户端：`com.bookshop.admin.service.ai.OpenAiCompatibleClient`
- 业务逻辑：`com.bookshop.admin.service.ai.PortalAiChatService`
- 接口：`POST /api/portal/ai/chat`、`GET /api/portal/ai/status`
