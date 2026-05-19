@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."

set "TOMCAT_WEBAPPS=D:\apache-tomcat-9.0.93-windows-x64\apache-tomcat-9.0.93\webapps"
set "WAR_SRC=%CD%\target\Onlinebookshop_Manager.war"
set "APP_NAME=Onlinebookshop_Manager_war"

if not exist "%WAR_SRC%" (
  echo [错误] 找不到 %WAR_SRC%
  echo 请先运行 scripts\build-war-java8.bat
  exit /b 1
)

echo 停止 Tomcat 后按任意键继续部署...
pause >nul

if exist "%TOMCAT_WEBAPPS%\%APP_NAME%" rmdir /s /q "%TOMCAT_WEBAPPS%\%APP_NAME%"
if exist "%TOMCAT_WEBAPPS%\%APP_NAME%.war" del /f /q "%TOMCAT_WEBAPPS%\%APP_NAME%.war"

copy /y "%WAR_SRC%" "%TOMCAT_WEBAPPS%\%APP_NAME%.war"
echo [完成] 已复制到 %TOMCAT_WEBAPPS%\%APP_NAME%.war
echo 请启动 Tomcat，访问 http://localhost:8080/%APP_NAME%/
pause
