@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."

echo ========================================
echo  Tomcat 部署诊断（Spring Boot 能启、Tomcat 不能时运行）
echo ========================================
echo.

echo [1] 8080 端口
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if errorlevel 1 (
  echo   空闲 - 可以启动 Tomcat
) else (
  echo   已被占用 - 若 Spring Boot 在跑，请先停止再启 Tomcat
  netstat -ano | findstr ":8080" | findstr "LISTENING"
)
echo.

echo [2] WAR / 展开目录
if exist "target\Onlinebookshop_Manager.war" (
  echo   OK  target\Onlinebookshop_Manager.war
) else (
  echo   缺少 WAR - 请在 IDEA 运行「仅打包WAR」或 scripts\build-war-java8.bat
)
if exist "target\Onlinebookshop_Manager\WEB-INF\classes" (
  echo   OK  target\Onlinebookshop_Manager\ 展开目录
) else (
  echo   缺少展开目录 - Maven package 未成功
)
echo.

echo [3] class 文件版本（应为 52 = Java 8）
if exist "target\classes\com\bookshop\admin\BookshopAdminApplication.class" (
  call scripts\verify-jdk8-classes.bat 2>nul
) else (
  echo   未编译，请先打包
)
echo.

echo [4] MySQL 3306
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul
if errorlevel 1 (echo   未启动) else (echo   已启动)
echo.

echo [5] IDEA Tomcat 检查清单
echo   - Deployment: Onlinebookshop_Manager:war exploded
echo   - Application context: /Onlinebookshop_Manager
echo   - Server JRE: JDK 1.8
echo   - Maven Runner JRE: 1.8
echo   - 启动前先停 Spring Boot
echo.
echo 详细步骤见: scripts\Tomcat-IDEA配置指南.md
echo.
pause
