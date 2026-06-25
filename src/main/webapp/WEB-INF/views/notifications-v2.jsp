<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.time.format.DateTimeFormatter,java.util.List,com.example.demo.model.Notification,com.example.demo.util.TextUtils" %>
<%
    List<Notification> notifications = (List<Notification>) request.getAttribute("notifications");
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
<%@ include file="/WEB-INF/views/fragments/header-v2.jspf" %>
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
        </section>
    <% } %>
</main>
</body>
</html>
