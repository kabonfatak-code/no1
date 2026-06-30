<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.example.demo.util.TextUtils" %>
<%
    String error = (String) request.getAttribute("error");
    String savedUsername = (String) request.getAttribute("username");
    String savedPhone = (String) request.getAttribute("phone");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>注册 - BBS 论坛</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main class="page narrow">
    <section class="form-card">
        <h1>注册</h1>
        <% if (error != null) { %><div class="error"><%= TextUtils.escapeHtml(error) %></div><% } %>
        <form method="post" action="<%= ctx %>/register" accept-charset="UTF-8">
            <label>用户名
                <input type="text" name="username" value="<%= TextUtils.escapeHtml(savedUsername) %>" maxlength="20" required>
            </label>
            <label>密码
                <input type="password" name="password" maxlength="32" minlength="6" required>
            </label>
            <label>电话
                <div class="inline-control">
                    <input id="registerPhone" type="text" name="phone" value="<%= TextUtils.escapeHtml(savedPhone) %>" maxlength="20" required>
                    <button class="button" type="button" data-sms-purpose="REGISTER" data-phone-input="registerPhone">获取验证码</button>
                </div>
            </label>
            <label>短信验证码
                <input type="text" name="smsCode" maxlength="8" required>
            </label>
            <div class="sms-result" id="smsResult"></div>
            <button class="button primary full" type="submit">完成注册</button>
        </form>
    </section>
</main>
</body>
</html>
