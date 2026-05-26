# Tomcat 9 在 IDEA 中部署本项目的配置指南

**日常开发推荐直接用「启动后端 SpringBoot」**，与 Vite 联调完全一致，不必强求 Tomcat。

若毕业答辩/说明书要求演示「WAR + Tomcat」部署，按下面检查。

---

## 一、先确认 Spring Boot 已停

8080 只能被一个进程占用。若 Spring Boot 已在跑，Tomcat 会启动失败或端口冲突。

- 停止「启动后端 SpringBoot」
- 或运行 `scripts\检查端口.bat` 确认 8080 空闲后再启 Tomcat

---

## 二、IDEA 必须全程使用 JDK 8

| 位置 | 设置 |
|------|------|
| File → Project Structure → Project | SDK = **1.8** |
| Settings → Build → Maven → Runner | JRE = **1.8** |
| Tomcat 运行配置 → Server | JRE = **1.8**（不要用 JRE 目录） |

若用 JDK 11 编译，Tomcat 会报：`UnsupportedClassVersionError ... class file version 55.0`

先运行：`scripts\仅打包WAR`（或 `scripts\build-war-java8.bat`），再运行 `scripts\verify-jdk8-classes.bat` 确认 class 版本为 52。

---

## 三、配置 Tomcat 运行项（与仓库模板一致）

1. **Run → Edit Configurations → + → Tomcat Server → Local**
2. **Server** 选项卡  
   - HTTP port: `8080`  
   - JRE: `1.8`
3. **Deployment** 选项卡  
   - 点 **+** → **Artifact** → 选 **`Onlinebookshop_Manager:war exploded`**  
     - 若没有此项：先执行下面「四、生成 Artifact」  
   - **Application context** 必须为：`/Onlinebookshop_Manager`  
     - ❌ 不要用 `/Onlinebookshop_Manager_war`（除非同步改前端 `.env` 的 `VITE_BACKEND_CONTEXT`）
4. **Before launch**（启动前任务）  
   - 保留 **Build Artifacts**  
   - 增加 **Run Maven Goal**：`clean package -DskipTests`  
5. 应用后运行 **Tomcat 9.0.93**

仓库已提供模板：`.idea/runConfigurations/Tomcat_9_0_93.xml`（需在 IDEA 里选好本机 Tomcat 安装目录，名称与 `APPLICATION_SERVER_NAME` 一致）。

---

## 四、没有 Artifact 时手动添加

1. **File → Project Structure → Artifacts → +**
2. 选 **Web Application: Exploded → From Maven…**  
   或 **Web Application: Exploded → Empty**，输出目录设为：  
   `项目根/target/Onlinebookshop_Manager`
3. 先命令行打包一次：运行 IDEA 配置 **「仅打包WAR」** 或 `scripts\build-war-java8.bat`
4. 确认存在目录 `target\Onlinebookshop_Manager\WEB-INF\classes`

---

## 五、启动成功如何验证

浏览器打开（应返回 JSON）：

```
http://127.0.0.1:8080/Onlinebookshop_Manager/api/portal/categories
```

首页：

```
http://127.0.0.1:8080/Onlinebookshop_Manager/
```

然后再开前端：`npm run dev`，访问 `http://127.0.0.1:5173`。

---

## 六、常见报错对照

| 现象 | 原因 | 处理 |
|------|------|------|
| 部署失败 / Artifact not found | 未打包或未配置 Artifact | 先 `仅打包WAR`，再配置 Deployment |
| Address already in use :8080 | Spring Boot 或其它程序占端口 | 停掉 8080 进程 |
| class file version 55.0 | 用 JDK11+ 编译 | Maven/IDEA 全部改为 JDK8 后 `mvn clean package` |
| 404，路径多一层 | Tomcat 用了 dev 配置又设了 context | 外置 Tomcat 必须用 **prod**（`ServletInitializer` 已强制 prod） |
| 启动后马上退出 | MySQL 未开或库未导入 | 启动 MySQL，导入 `sql/bookshop_admin.sql` |
| 中文路径下编译失败 | javac 找不到源文件 | 用 IDEA 的 Maven 打包，或把项目移到纯英文路径 |

---

## 七、不用 IDEA 时的手动部署

1. `scripts\build-war-java8.bat`
2. 修改 `scripts\deploy-tomcat-war.bat` 里的 `TOMCAT_WEBAPPS` 为你的 Tomcat `webapps` 路径
3. 停止 Tomcat → 运行 deploy 脚本 → 再启动 Tomcat  
4. 访问：`http://localhost:8080/Onlinebookshop_Manager_war/`（脚本默认用 `_war` 作目录名，与 IDEA 的 `/Onlinebookshop_Manager` 不同，二选一统一即可）

---

**结论**：开发联调用 **SpringBoot + Vite** 即可；Tomcat 按本文检查 JDK8、Artifact、context、8080 端口与 MySQL。
