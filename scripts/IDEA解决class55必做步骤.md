# IDEA 解决 class 55.0（必做步骤）

报错含义：**class 是用 JDK 11 编译的（55.0），却用 Java 8 启动（只认 52.0）**。

---

## 第一步：删除 target

路径（与 `pom.xml` 同级）：

```
Onlinebookshop_Manager\target
```

整个文件夹删除。

---

## 第二步：不要用「Build Project」

请 **不要** 点菜单 **Build → Build Project**（容易用成 JDK 11 编译）。

只用 **Maven** 编译：

1. 右侧 **Maven** 工具窗口  
2. **Lifecycle** → 双击 **clean**  
3. 再双击 **compile**  

或双击运行：`scripts\修复class55并启动.bat`

---

## 第三步：Maven 使用哪个 JDK

**Settings → Build, Execution, Deployment → Build Tools → Maven → Runner**

- **JRE** 选 **1.8**（推荐）  
- 或选 **11** 也可以（项目 `pom.xml` 已加配置，用 11 编译也会生成 **Java 8 字节码**）

---

## 第四步：运行配置

**Run → Edit Configurations → 启动后端 SpringBoot**

- **JRE**：必须是 **1.8**（与 Tomcat 一致）  
- 启动前已有 **Maven: clean compile**（不要关）

然后点运行。

---

## 第五步：确认 Project SDK

**File → Project Structure → Project**

- **SDK**：**1.8**  
- **Language level**：**8**

**Modules → Onlinebookshop_Manager → Language level**：**8**

---

## 仍失败时

1. **File → Invalidate Caches → Invalidate and Restart**  
2. 重启后再 **Maven clean → compile**  
3. 确认没有第二个 `out` 目录在参与运行（本项目输出应在 `target/classes`）

---

## 版本对照

| class 文件版本 | JDK |
|----------------|-----|
| 52.0 | Java 8 |
| 55.0 | Java 11 |

运行 Spring Boot 的 JRE 必须是 **8**；编译可以是 8，或用 11+Maven（已配置 `release 8`）。
