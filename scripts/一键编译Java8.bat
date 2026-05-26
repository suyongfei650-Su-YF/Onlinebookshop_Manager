@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."

set "JAVA_HOME=D:\Java Web\Java jdk\Java\jdk-1.8"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [错误] 未找到 JDK 8: %JAVA_HOME%
  pause
  exit /b 1
)

if exist "target" rmdir /s /q "target"

set "MVN=mvn"
where mvn >nul 2>&1
if errorlevel 1 set "MVN=D:\IDEAIU\IntelliJ IDEA 2024.1.4\plugins\maven\lib\maven3\bin\mvn.cmd"

echo JAVA_HOME=%JAVA_HOME%
java -version
echo.
echo 正在编译（必须出现 debug target 1.8 / major version 52）...
call "%MVN%" clean compile -DskipTests
if errorlevel 1 (
  echo [失败] 请查看上方错误
  pause
  exit /b 1
)

"%JAVA_HOME%\bin\javap" -verbose target\classes\com\bookshop\admin\BookshopAdminApplication.class | findstr "major version"
echo.
echo [成功] 请用 IDEA 运行「启动后端 SpringBoot」，勿使用 Build Project。
pause
