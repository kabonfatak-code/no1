<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.example.demo.util.TextUtils" %>
<%
    String error = (String) request.getAttribute("error");
    String savedUsername = (String) request.getAttribute("username");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>用户登录 - 小型论坛 BBS</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main class="page narrow">
    <section class="form-card">
        <h1>用户登录</h1>
        <% if (error != null) { %>
            <div class="error"><%= TextUtils.escapeHtml(error) %></div>
        <% } %>
        <form method="post" action="<%= ctx %>/login" accept-charset="UTF-8">
            <label>
                用户名
                <input type="text" name="username" value="<%= TextUtils.escapeHtml(savedUsername) %>" required>
            </label>
            <label>
                密码
                <input type="password" name="password" required>
            </label>
            <button class="button primary full" type="submit">登录</button>
        </form>
        <p class="form-link">没有账号？<a href="<%= ctx %>/register">立即注册</a></p>
    </section>
</main>
</body>
</html>
