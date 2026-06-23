<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.example.demo.util.TextUtils" %>
<%
    String error = (String) request.getAttribute("error");
    String savedUsername = (String) request.getAttribute("username");
    String savedRealName = (String) request.getAttribute("realName");
    String savedEmail = (String) request.getAttribute("email");
    String savedGender = (String) request.getAttribute("gender");
    String savedPhone = (String) request.getAttribute("phone");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>用户注册 - 小型论坛 BBS</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main class="page narrow">
    <section class="form-card">
        <h1>用户注册</h1>
        <% if (error != null) { %>
            <div class="error"><%= TextUtils.escapeHtml(error) %></div>
        <% } %>
        <form method="post" action="<%= ctx %>/register" accept-charset="UTF-8">
            <label>
                用户名
                <input type="text" name="username" value="<%= TextUtils.escapeHtml(savedUsername) %>" maxlength="20" required>
            </label>
            <label>
                密码
                <input type="password" name="password" maxlength="32" required>
            </label>
            <label>
                姓名
                <input type="text" name="realName" value="<%= TextUtils.escapeHtml(savedRealName) %>" maxlength="30" required>
            </label>
            <label>
                邮箱
                <input type="email" name="email" value="<%= TextUtils.escapeHtml(savedEmail) %>" maxlength="80" required>
            </label>
            <label>
                性别
                <select name="gender">
                    <option value="保密" <%= "保密".equals(savedGender) || savedGender == null ? "selected" : "" %>>保密</option>
                    <option value="男" <%= "男".equals(savedGender) ? "selected" : "" %>>男</option>
                    <option value="女" <%= "女".equals(savedGender) ? "selected" : "" %>>女</option>
                </select>
            </label>
            <label>
                电话
                <input type="text" name="phone" value="<%= TextUtils.escapeHtml(savedPhone) %>" maxlength="30">
            </label>
            <button class="button primary full" type="submit">注册</button>
        </form>
        <p class="form-link">已有账号？<a href="<%= ctx %>/login">去登录</a></p>
    </section>
</main>
</body>
</html>
