@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."

echo ========================================
echo  使用 JDK 8 打包 WAR（class 版本 52）
echo ========================================

if defined JAVA_HOME_8 set "JAVA_HOME=%JAVA_HOME_8%"
if not defined JAVA_HOME if exist "C:\Program Files\Java\jdk1.8.0_421" set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_421"
if not defined JAVA_HOME if exist "C:\Program Files\Java\jre1.8.0_421" set "JAVA_HOME=C:\Program Files\Java\jre1.8.0_421"

if not defined JAVA_HOME (
  echo [错误] 未找到 JDK 8，请安装 JDK 1.8 并设置 JAVA_HOME 或 JAVA_HOME_8
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo JAVA_HOME=%JAVA_HOME%
java -version 2>&1 | findstr /i "version"
echo.

where mvn >nul 2>&1
if errorlevel 1 (
  echo [错误] 未找到 mvn 命令，请安装 Maven 并加入 PATH
  exit /b 1
)

if exist "target\Onlinebookshop_Manager" rmdir /s /q "target\Onlinebookshop_Manager"
call mvn clean package -DskipTests
if errorlevel 1 (
  echo [错误] Maven 打包失败
  exit /b 1
)

echo.
echo [成功] target\Onlinebookshop_Manager.war
echo [成功] target\Onlinebookshop_Manager\  （Tomcat 可部署此目录）
echo.
echo 下一步：运行 scripts\deploy-tomcat-war.bat 或重启 IDEA 中的 Tomcat
pause
