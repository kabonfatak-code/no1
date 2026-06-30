<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.example.demo.model.Post,com.example.demo.util.TextUtils" %>
<%
    Post post = (Post) request.getAttribute("post");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>编辑留言 - BBS 论坛</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main class="page">
    <section class="form-card wide">
        <h1>编辑留言</h1>
        <% if (error != null) { %><div class="error"><%= TextUtils.escapeHtml(error) %></div><% } %>
        <form method="post" action="<%= ctx %>/post/edit">
            <input type="hidden" name="id" value="<%= post.getId() %>">
            <div class="form-grid">
                <label>主题
                    <input type="text" name="topic" value="<%= TextUtils.escapeHtml(post.getTopic()) %>" maxlength="30" required>
                </label>
            </div>
            <label>正文
                <textarea name="content" rows="14" maxlength="8000" required><%= TextUtils.escapeHtml(post.getContent()) %></textarea>
            </label>
            <div class="form-actions">
                <a class="button" href="<%= ctx %>/post/detail?id=<%= post.getId() %>">取消</a>
                <button class="button primary" type="submit">保存</button>
            </div>
        </form>
    </section>
</main>
</body>
</html>
