# class file version 55.0（JDK 11）与 Java 8 运行冲突

## 含义

- **55.0** = 用 **JDK 11** 编译出的 `.class`
- 你用 **JDK 8（1.8.0_421）** 启动 Spring Boot / Tomcat，只能加载 **52.0** 及以下

因此出现：`UnsupportedClassVersionError ... class file version 55.0`

---

## 必须做的三步（顺序不能乱）

### 1. 删掉旧编译结果

在项目根目录**手动删除整个 `target` 文件夹**（或在 IDEA：**Build → Clean Project**）。

> 只要 `target\classes` 里还残留 JDK 11 编出来的 class，启动就会继续报错。

### 2. 全程只用 JDK 8 编译

| 位置 | 设置 |
|------|------|
| **File → Project Structure → Project** | **SDK = 1.8**，Language level = 8 |
| **File → Settings → Build, Execution, Deployment → Build Tools → Maven → Runner** | **JRE = 1.8**（不要用 Project JDK 若其为 11） |
| **File → Settings → Build, Execution, Deployment → Compiler → Java Compiler** | **Project bytecode version = 8** |

仓库已增加 `.idea/compiler.xml`，将字节码目标固定为 **1.8**。

### 3. 用 Maven 重新编译后再启动

- IDEA 运行配置 **「启动后端 SpringBoot」** 已带 **Maven `compile -DskipTests`**，请先 **Build → Rebuild Project** 或删除 `target` 后再运行。
- 或在资源管理器中双击运行：**`scripts\fix-java8-build.bat`**（会删 `target` 并用本机 `JAVA_HOME` 下的 Java 执行 `mvn clean compile`；需已安装 Maven 且在 PATH 中）。

验证：在项目根执行 **`scripts\verify-jdk8-classes.bat`**，应对 `AdminAccountController.class` 显示 **major version 52**。

---

## 常见误操作

- 用 **JDK 11/17** 的 IDEA 默认编译器点了 **Build**，把 `target` 又编成 55.0，而运行仍用 8 → 再次报错。
- 只改 `pom.xml` 里的 `source/target`，**不删 `target`** → 旧 class 仍在。

---

## 若本机没有 Maven

在 IDEA 中：**右侧 Maven → Lifecycle → clean**，再 **compile**（确认 Maven Runner 的 JRE 为 1.8），然后运行 Spring Boot。

---

修复完成后，`AdminAccountController` 等类应全部为 **52.0**，与 Tomcat / Spring Boot on Java 8 一致。
