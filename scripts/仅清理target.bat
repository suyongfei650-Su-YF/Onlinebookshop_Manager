@echo off
chcp 65001 >nul
cd /d "%~dp0.."
if exist "target" (
  echo 正在删除 target（清除错误的 class 55.0）...
  rmdir /s /q "target"
  echo 已删除。请在 IDEA 中用 Maven 执行 clean compile，勿单独 Build Project。
) else (
  echo target 不存在，无需清理。
)
pause
