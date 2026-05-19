<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8"/>
  <title>馆藏书屋 · 管理端</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 720px; margin: 2rem auto; padding: 0 1rem; line-height: 1.6; }
    code { background: #f4f4f4; padding: 0.1rem 0.35rem; border-radius: 4px; }
  </style>
</head>
<body>
<h1>Onlinebookshop_Manager</h1>
<p>管理员 JSON 接口前缀：<code>/Onlinebookshop_Manager/api/admin/</code></p>
<ul>
  <li><code>POST /api/admin/login</code> 管理员登录（JSON：username, password）</li>
  <li><code>POST /api/admin/logout</code> 退出</li>
  <li><code>GET /api/admin/dashboard</code> 仪表盘统计（需登录）</li>
  <li><code>GET /api/admin/books?page=1&amp;size=10&amp;keyword=</code> 图书分页</li>
  <li><code>GET /api/admin/categories</code> 分类列表</li>
  <li><code>GET /api/admin/orders</code> 订单列表</li>
  <li><code>GET /api/admin/customers</code> 读者用户列表</li>
</ul>
<p>Vue 管理员端：在 <code>manager-ui</code> 目录执行 <code>npm run dev</code>（已配置代理到本机 Tomcat）。生产构建后将 <code>dist</code> 静态资源部署到本应用下即可。</p>
<p>数据库：执行项目根目录 <code>sql/bookshop_admin.sql</code>，并配置 <code>src/main/resources/db.properties</code>。</p>
</body>
</html>
