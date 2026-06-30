<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.time.format.DateTimeFormatter,java.util.List,com.example.demo.model.Notification,com.example.demo.util.TextUtils" %>
<%
    List<Notification> notifications = (List<Notification>) request.getAttribute("notifications");
    Integer currentPageValue = (Integer) request.getAttribute("currentPage");
    Integer totalPagesValue = (Integer) request.getAttribute("totalPages");
    int currentPage = currentPageValue == null ? 1 : currentPageValue;
    int totalPages = totalPagesValue == null ? 1 : totalPagesValue;
    String pageUrlPrefix = request.getContextPath() + "/notifications?page=";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>信息通知 - BBS 论坛</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main class="page">
    <section class="page-heading">
        <div>
            <h1>信息通知</h1>
        </div>
    </section>
    <% if (notifications == null || notifications.isEmpty()) { %>
        <section class="empty-state">暂无通知</section>
    <% } else { %>
        <section class="post-list">
            <% for (Notification notification : notifications) { %>
                <a class="post-card notification-card <%= notification.isRead() ? "" : "unread" %>" href="<%= notification.getTargetUrl(ctx) %>">
                    <div class="post-card-head">
                        <strong><%= TextUtils.escapeHtml(notification.getMessage()) %></strong>
                        <time><%= formatter.format(notification.getCreatedAt()) %></time>
                    </div>
                    <p class="post-meta">点击查看对应留言或评论</p>
                </a>
            <% } %>
            <% if (totalPages > 1) { %>
                <nav class="pagination" aria-label="通知分页">
                    <% if (currentPage > 1) { %>
                        <a class="page-link" href="<%= pageUrlPrefix %><%= currentPage - 1 %>">上一页</a>
                    <% } else { %>
                        <span class="page-link disabled">上一页</span>
                    <% } %>
                    <% for (int i = 1; i <= totalPages; i++) { %>
                        <% if (i == currentPage) { %>
                            <span class="page-link active"><%= i %></span>
                        <% } else { %>
                            <a class="page-link" href="<%= pageUrlPrefix %><%= i %>"><%= i %></a>
                        <% } %>
                    <% } %>
                    <% if (currentPage < totalPages) { %>
                        <a class="page-link" href="<%= pageUrlPrefix %><%= currentPage + 1 %>">下一页</a>
                    <% } else { %>
                        <span class="page-link disabled">下一页</span>
                    <% } %>
                </nav>
            <% } %>
        </section>
    <% } %>
</main>
</body>
</html>
