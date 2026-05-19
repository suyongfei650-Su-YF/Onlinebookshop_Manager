@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."

set "CLASS_FILE=target\classes\com\bookshop\admin\BookshopAdminApplication.class"
if not exist "%CLASS_FILE%" (
  echo [错误] 未找到 %CLASS_FILE%，请先执行 scripts\fix-java8-build.bat
  exit /b 1
)

echo 检查 class 文件版本（应为 major version 52 = Java 8）...
javap -verbose "%CLASS_FILE%" 2>nul | findstr /C:"major version"
if errorlevel 1 (
  echo [提示] 无法运行 javap，请确认 JAVA_HOME 指向 JDK 8
  exit /b 1
)

javap -verbose "%CLASS_FILE%" | findstr /C:"major version: 52" >nul
if errorlevel 1 (
  echo.
  echo [失败] 当前 class 不是 Java 8 编译（若显示 55 则为 Java 11）
  echo 请用 JDK 8 执行: scripts\fix-java8-build.bat
  exit /b 1
)

echo [成功] class 版本为 52（Java 8），可以启动 Tomcat / Spring Boot
pause
