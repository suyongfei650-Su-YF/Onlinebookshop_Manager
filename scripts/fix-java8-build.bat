@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."

echo ========================================
echo  修复 class 55.0 / 应为 52.0（必须用 JDK8 编译）
echo ========================================

if defined JAVA_HOME_8 set "JAVA_HOME=%JAVA_HOME_8%"
if not defined JAVA_HOME if exist "C:\Program Files\Java\jdk1.8.0_421" set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_421"
if not defined JAVA_HOME if exist "C:\Program Files\Java\jre1.8.0_421" set "JAVA_HOME=C:\Program Files\Java\jre1.8.0_421"

if not defined JAVA_HOME (
  echo [错误] 未找到 JDK 8，请安装 JDK 1.8 并设置 JAVA_HOME
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo JAVA_HOME=%JAVA_HOME%
java -version 2>&1

echo.
echo [重要] 若 IDEA 报「类文件版本 55.0 应为 52.0」，必须先删 target 再编译，勿单独 Build Project。
echo.
echo [1/2] 删除 target（清除 Java 11 编译残留）...
if exist "target" rmdir /s /q "target"
if exist "target\Onlinebookshop_Manager" rmdir /s /q "target\Onlinebookshop_Manager"

echo [2/2] 使用 JDK 8 重新编译...
where mvn >nul 2>&1
if errorlevel 1 (
  echo [提示] 未找到 mvn，请在 IDEA 中：
  echo   - Project Structure - Project SDK = 1.8
  echo   - Settings - Java Compiler - bytecode = 8
  echo   - Build - Rebuild Project
  echo 并确认不要用 JDK 11/17 做 Build。
  pause
  exit /b 0
)

call mvn clean compile -DskipTests
if errorlevel 1 (
  echo [错误] 编译失败。若提示 JDK 版本过高，请把 JAVA_HOME 指向 JDK 8。
  pause
  exit /b 1
)

echo.
echo [成功] 已用 JDK 8 生成 target\classes（class 52.0）
echo IDEA 中请将 Project SDK 设为 1.8 后再 Rebuild，避免 IDEA 又用 JDK 11 覆盖。
pause
