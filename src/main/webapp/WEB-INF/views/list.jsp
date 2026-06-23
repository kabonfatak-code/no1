<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.time.format.DateTimeFormatter,java.util.List,com.example.demo.model.Post,com.example.demo.util.TextUtils" %>
<%
    List<Post> posts = (List<Post>) request.getAttribute("posts");
    Integer currentPage = (Integer) request.getAttribute("currentPage");
    Integer totalPages = (Integer) request.getAttribute("totalPages");
    Integer totalCount = (Integer) request.getAttribute("totalCount");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>留言列表 - 小型论坛 BBS</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main class="page">
    <section class="page-heading">
        <div>
            <h1>留言列表</h1>
            <p>共 <%= totalCount %> 条留言</p>
        </div>
        <% if (loginUser != null) { %>
            <a class="button primary" href="<%= ctx %>/post/new">发表留言</a>
        <% } %>
    </section>

    <% if (posts == null || posts.isEmpty()) { %>
        <section class="empty-state">暂无留言</section>
    <% } else { %>
        <section class="post-list">
            <% for (Post post : posts) { %>
                <article class="post-row">
                    <div class="post-summary">
                        <a class="post-title" href="<%= ctx %>/post/detail?id=<%= post.getId() %>"><%= TextUtils.escapeHtml(post.getTitle()) %></a>
                        <div class="post-meta">作者：<%= TextUtils.escapeHtml(post.getAuthorUsername()) %></div>
                    </div>
                    <time class="post-time" datetime="<%= post.getCreatedAt() %>"><%= formatter.format(post.getCreatedAt()) %></time>
                    <% if (loginUser != null && loginUser.isAdmin()) { %>
                        <form class="inline-form" method="post" action="<%= ctx %>/post/delete" onsubmit="return confirm('确认删除该留言？');">
                            <input type="hidden" name="id" value="<%= post.getId() %>">
                            <button class="button danger" type="submit">删除</button>
                        </form>
                    <% } %>
                </article>
            <% } %>
        </section>
    <% } %>

    <% if (totalPages > 1) { %>
        <nav class="pagination" aria-label="分页">
            <% if (currentPage > 1) { %>
                <a href="<%= ctx %>/posts?page=<%= currentPage - 1 %>">上一页</a>
            <% } else { %>
                <span class="disabled">上一页</span>
            <% } %>

            <% for (int i = 1; i <= totalPages; i++) { %>
                <% if (i == currentPage) { %>
                    <span class="current"><%= i %></span>
                <% } else { %>
                    <a href="<%= ctx %>/posts?page=<%= i %>"><%= i %></a>
                <% } %>
            <% } %>

            <% if (currentPage < totalPages) { %>
                <a href="<%= ctx %>/posts?page=<%= currentPage + 1 %>">下一页</a>
            <% } else { %>
                <span class="disabled">下一页</span>
            <% } %>
        </nav>
    <% } %>
</main>
</body>
</html>
