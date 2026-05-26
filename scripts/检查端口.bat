@echo off
chcp 65001 >nul
echo ========================================
echo  网上书城 - 本地端口检查
echo ========================================
echo.

echo [前端 Vite] 应监听 5173（页面地址 http://localhost:5173）
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul
if errorlevel 1 (
  echo   状态: 未启动 - 请在 manager-ui 目录执行 npm run dev
) else (
  echo   状态: 已启动
  netstat -ano | findstr ":5173" | findstr "LISTENING"
)

echo.
echo [后端 Tomcat/SpringBoot] 应监听 8080
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if errorlevel 1 (
  echo   状态: 未启动 ^(这就是 localhost 拒绝连接的原因^)
  echo   解决: 在 IDEA 中运行「启动后端 SpringBoot」或「Tomcat 9.0.93」
  echo         前端出现 HTTP 502 也是这个原因（只开了 5173 没开 8080）
  echo   启动成功后浏览器打开:
  echo   http://localhost:8080/Onlinebookshop_Manager/api/portal/categories
) else (
  echo   状态: 已启动
  netstat -ano | findstr ":8080" | findstr "LISTENING"
)

echo.
echo [MySQL] 应监听 3306
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul
if errorlevel 1 (
  echo   状态: 未启动 - 请先启动 MySQL 并导入 sql/bookshop_admin.sql
) else (
  echo   状态: 已启动
)

echo.
echo.
echo [开发推荐] 只运行「启动后端 SpringBoot」+ npm run dev，不必配置 Tomcat
echo [Tomcat 失败] 运行 scripts\诊断Tomcat部署.bat，详见 scripts\Tomcat-IDEA配置指南.md
echo.
echo 正确访问方式:
echo   用户/管理页面: http://127.0.0.1:5173  或  http://localhost:5173
echo   管理后台登录:  http://127.0.0.1:5173/admin/login
echo.
echo 注意: 只访问 http://127.0.0.1 或 http://127.0.0.1:8080 会「拒绝连接」
echo       必须带端口 5173（网页）或先启动 8080 后端
echo.
pause
