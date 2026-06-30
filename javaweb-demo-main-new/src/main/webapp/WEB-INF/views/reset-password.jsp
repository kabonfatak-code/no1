<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.example.demo.util.TextUtils" %>
<%
    String error = (String) request.getAttribute("error");
    String savedPhone = (String) request.getAttribute("phone");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>找回密码 - BBS 论坛</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main class="page narrow">
    <section class="form-card">
        <h1>找回密码</h1>
        <% if (error != null) { %><div class="error"><%= TextUtils.escapeHtml(error) %></div><% } %>
        <form method="post" action="<%= ctx %>/password/reset">
            <label>电话
                <div class="inline-control">
                    <input id="resetPhone" type="text" name="phone" value="<%= TextUtils.escapeHtml(savedPhone) %>" required>
                    <button class="button" type="button" data-sms-purpose="RESET" data-phone-input="resetPhone">获取验证码</button>
                </div>
            </label>
            <label>短信验证码
                <input type="text" name="smsCode" required>
            </label>
            <label>新密码
                <input type="password" name="password" minlength="6" maxlength="32" required>
            </label>
            <div class="sms-result" id="smsResult"></div>
            <button class="button primary full" type="submit">重置密码</button>
        </form>
    </section>
</main>
</body>
</html>
