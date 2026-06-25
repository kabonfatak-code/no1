<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.time.format.DateTimeFormatter,java.util.List,com.example.demo.model.User,com.example.demo.model.Report,com.example.demo.util.TextUtils" %>
<%
    List<User> users = (List<User>) request.getAttribute("users");
    List<Report> reports = (List<Report>) request.getAttribute("reports");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>管理员后台 - BBS 论坛</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/fragments/header-v2.jspf" %>
<main class="page">
    <section class="page-heading">
        <div>
            <h1>管理员后台</h1>
            <p>管理员不能举报，也不能封禁自己的账号。</p>
        </div>
    </section>

    <section class="admin-section">
        <h2>举报情况</h2>
        <% if (reports == null || reports.isEmpty()) { %>
            <div class="empty-state small">暂无举报</div>
        <% } else { %>
            <% for (Report report : reports) { %>
                <article class="post-card">
                    <div class="post-card-head">
                        <div>
                            <a class="post-title" href="<%= ctx %>/post/detail?id=<%= report.getPostId() %><%= report.getTargetAnchor() %>">
                                <%= "comment".equals(report.getTargetType()) ? "评论举报" : "留言举报" %>
                            </a>
                            <div class="post-meta">
                                <span>被举报用户：<%= TextUtils.escapeHtml(report.getTargetUsername()) %></span>
                                <span>举报人数：<%= report.getReportCount() %></span>
                                <span>举报人：<%= TextUtils.escapeHtml(report.getReporterUsername()) %></span>
                                <time><%= formatter.format(report.getCreatedAt()) %></time>
                            </div>
                        </div>
                        <span class="tag"><%= report.isHandled() ? "已处理" : "待处理" %></span>
                    </div>
                    <p class="post-excerpt">原因：<%= TextUtils.escapeHtml(report.getReason()) %></p>
                    <% if (!report.isHandled()) { %>
                        <form method="post" action="<%= ctx %>/admin/action" class="report-handle-form">
                            <input type="hidden" name="action" value="report">
                            <input type="hidden" name="targetType" value="<%= report.getTargetType() %>">
                            <input type="hidden" name="targetId" value="<%= "comment".equals(report.getTargetType()) ? report.getCommentId() : report.getPostId() %>">
                            <label>封号时长
                                <select name="banDays">
                                    <option value="0">不封号，仅标记处理</option>
                                    <option value="1">1 天</option>
                                    <option value="7">7 天</option>
                                    <option value="30">30 天</option>
                                    <option value="365">365 天</option>
                                </select>
                            </label>
                            <button class="button primary" type="submit">处理举报</button>
                        </form>
                    <% } %>
                </article>
            <% } %>
        <% } %>
    </section>

    <section class="admin-section">
        <h2>用户管理</h2>
        <div class="table-wrap">
            <table>
                <thead>
                <tr>
                    <th>用户</th>
                    <th>电话</th>
                    <th>类型</th>
                    <th>状态</th>
                    <th>操作</th>
                </tr>
                </thead>
                <tbody>
                <% for (User item : users) { %>
                    <tr>
                        <form method="post" action="<%= ctx %>/admin/action">
                            <td><%= TextUtils.escapeHtml(item.getUsername()) %></td>
                            <td><input type="text" name="phone" value="<%= TextUtils.escapeHtml(item.getPhone()) %>"></td>
                            <td><%= TextUtils.escapeHtml(item.getRoleLabel()) %></td>
                            <%
                                String banState;
                                if (!item.isBanned()) {
                                    banState = "0:0";
                                } else if (item.getBannedUntil() == null) {
                                    banState = "1:0";
                                } else {
                                    long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
                                        java.time.LocalDateTime.now(), item.getBannedUntil());
                                    if (daysLeft <= 1) banState = "1:1";
                                    else if (daysLeft <= 7) banState = "1:7";
                                    else if (daysLeft <= 30) banState = "1:30";
                                    else banState = "1:365";
                                }
                            %>
                            <td>
                                <select name="banAction" <%= item.getId() == loginUser.getId() ? "disabled" : "" %>>
                                    <option value="0:0" <%= "0:0".equals(banState) ? "selected" : "" %>>正常</option>
                                    <option value="1:1" <%= "1:1".equals(banState) ? "selected" : "" %>>封禁 1 天</option>
                                    <option value="1:7" <%= "1:7".equals(banState) ? "selected" : "" %>>封禁 7 天</option>
                                    <option value="1:30" <%= "1:30".equals(banState) ? "selected" : "" %>>封禁 30 天</option>
                                    <option value="1:365" <%= "1:365".equals(banState) ? "selected" : "" %>>封禁 365 天</option>
                                    <option value="1:0" <%= "1:0".equals(banState) ? "selected" : "" %>>永久封禁</option>
                                </select>
                                <% if (item.getId() == loginUser.getId()) { %>
                                    <input type="hidden" name="banAction" value="0:0">
                                <% } %>
                                <input type="hidden" name="role" value="<%= item.getRole() %>">
                            </td>
                            <td>
                                <input type="hidden" name="action" value="user">
                                <input type="hidden" name="userId" value="<%= item.getId() %>">
                                <button class="button" type="submit">保存</button>
                            </td>
                        </form>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </section>
</main></body>
</html>
