@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."

echo ========================================
echo  修复 class 55.0 - 强制用 Java 8 字节码重新编译
echo ========================================
echo.

if defined JAVA_HOME_8 set "JAVA_HOME=%JAVA_HOME_8%"
if not defined JAVA_HOME if exist "D:\Java Web\Java jdk\Java\jdk-1.8" set "JAVA_HOME=D:\Java Web\Java jdk\Java\jdk-1.8"
if not defined JAVA_HOME if exist "D:\Java jdk\Java\jdk-1.8" set "JAVA_HOME=D:\Java jdk\Java\jdk-1.8"
if not defined JAVA_HOME if exist "C:\Program Files\Java\jdk1.8.0_421" set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_421"

set "MVN_CMD=mvn"
where mvn >nul 2>&1
if errorlevel 1 if exist "D:\IDEAIU\IntelliJ IDEA 2024.1.4\plugins\maven\lib\maven3\bin\mvn.cmd" set "MVN_CMD=D:\IDEAIU\IntelliJ IDEA 2024.1.4\plugins\maven\lib\maven3\bin\mvn.cmd"

if not defined JAVA_HOME (
  echo [错误] 未找到 JAVA_HOME，请安装 JDK 8 或在系统环境变量中设置 JAVA_HOME
  pause
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo 当前 JAVA_HOME=%JAVA_HOME%
java -version 2>&1
echo.

if exist "target" (
  echo 删除 target ...
  rmdir /s /q "target"
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [错误] JAVA_HOME 无效: %JAVA_HOME%
  pause
  exit /b 1
)

if "%MVN_CMD%"=="mvn" where mvn >nul 2>&1
if errorlevel 1 if not exist "D:\IDEAIU\IntelliJ IDEA 2024.1.4\plugins\maven\lib\maven3\bin\mvn.cmd" (
  echo.
  echo [未找到 mvn] 请改在 IDEA 中操作：
  echo   1. 删除 target 文件夹
  echo   2. 右侧 Maven - Lifecycle - clean
  echo   3. 再双击 compile
  echo   4. Settings - Maven - Runner - JRE 务必选 1.8
  echo   5. Settings - Build - Build Tools - Maven - 勾选 Delegate build to Maven
  echo   6. 运行「启动后端 SpringBoot」（已配置启动前 clean compile）
  echo.
  echo   切勿单独 Build - Build Project（会把 class 编成 55.0）
  echo   若仍报 55.0：先双击 scripts\仅清理target.bat 再 Maven compile
  pause
  exit /b 0
)

echo 执行 clean compile（MVN=%MVN_CMD%）...
call "%MVN_CMD%" clean compile -DskipTests
if errorlevel 1 (
  echo [错误] 编译失败，请看上方 Maven 输出
  pause
  exit /b 1
)

echo.
echo [成功] 编译完成。请用 IDEA 运行「启动后端 SpringBoot」（运行 JRE 须为 1.8）
echo.
pause
