<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.time.format.DateTimeFormatter,com.example.demo.model.Post,com.example.demo.util.TextUtils" %>
<%
    Post post = (Post) request.getAttribute("post");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= TextUtils.escapeHtml(post.getTitle()) %> - 小型论坛 BBS</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main class="page">
    <article class="detail">
        <div class="detail-head">
            <div>
                <h1><%= TextUtils.escapeHtml(post.getTitle()) %></h1>
                <p>作者：<%= TextUtils.escapeHtml(post.getAuthorUsername()) %> · 时间：<%= formatter.format(post.getCreatedAt()) %></p>
            </div>
            <% if (loginUser != null && loginUser.isAdmin()) { %>
                <form method="post" action="<%= ctx %>/post/delete" onsubmit="return confirm('确认删除该留言？');">
                    <input type="hidden" name="id" value="<%= post.getId() %>">
                    <button class="button danger" type="submit">删除</button>
                </form>
            <% } %>
        </div>
        <div class="detail-content"><%= TextUtils.escapeHtmlWithLineBreaks(post.getContent()) %></div>
    </article>
    <a class="back-link" href="<%= ctx %>/posts">返回留言列表</a>
</main>
</body>
</html>
