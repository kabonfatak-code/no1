<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.example.demo.util.TextUtils" %>
<%
    String error = (String) request.getAttribute("error");
    String savedUsername = (String) request.getAttribute("username");
    String savedPhone = (String) request.getAttribute("phone");
    String mode = (String) request.getAttribute("mode");
    boolean smsMode = "sms".equals(mode);
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>登录 - BBS 论坛</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main class="page narrow">
    <section class="form-card">
        <h1 data-login-heading><%= smsMode ? "手机号验证码登录" : "用户名密码登录" %></h1>
        <% if (error != null) { %><div class="error"><%= TextUtils.escapeHtml(error) %></div><% } %>

        <form class="login-panel <%= smsMode ? "hidden" : "" %>" data-login-panel="password" method="post" action="<%= ctx %>/login" accept-charset="UTF-8">
            <input type="hidden" name="mode" value="password">
            <label>用户名
                <input type="text" name="username" value="<%= TextUtils.escapeHtml(savedUsername) %>">
            </label>
            <label>密码
                <input type="password" name="password">
            </label>
            <button class="button primary full" type="submit">登录</button>
            <div class="login-switch-row">
                <a href="<%= ctx %>/password/reset">忘记密码？</a>
                <button type="button" class="link-button" data-login-switch="sms">手机号验证码登录</button>
            </div>
        </form>

        <form class="login-panel <%= smsMode ? "" : "hidden" %>" data-login-panel="sms" method="post" action="<%= ctx %>/login" accept-charset="UTF-8">
            <input type="hidden" name="mode" value="sms">
            <label>手机号
                <div class="inline-control">
                    <input id="loginPhone" type="text" name="phone" value="<%= TextUtils.escapeHtml(savedPhone) %>">
                    <button class="button" type="button" data-sms-purpose="LOGIN" data-phone-input="loginPhone">获取验证码</button>
                </div>
            </label>
            <label>短信验证码
                <input type="text" name="smsCode">
            </label>
            <div class="sms-result" id="smsResult"></div>
            <button class="button primary full" type="submit">验证码登录</button>
            <div class="login-switch-row">
                <a href="<%= ctx %>/register">没有账号？立即注册</a>
                <button type="button" class="link-button" data-login-switch="password">用户名密码登录</button>
            </div>
        </form>
    </section>
</main>
</body>
</html>
