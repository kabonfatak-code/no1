<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.time.format.DateTimeFormatter,java.util.List,com.example.demo.model.User,com.example.demo.model.Report,com.example.demo.util.TextUtils" %>
<%
    List<User> users = (List<User>) request.getAttribute("users");
    List<Report> reports = (List<Report>) request.getAttribute("reports");
    Integer reportCurrentPageValue = (Integer) request.getAttribute("reportCurrentPage");
    Integer reportTotalPagesValue = (Integer) request.getAttribute("reportTotalPages");
    Integer userCurrentPageValue = (Integer) request.getAttribute("userCurrentPage");
    Integer userTotalPagesValue = (Integer) request.getAttribute("userTotalPages");
    int reportCurrentPage = reportCurrentPageValue == null ? 1 : reportCurrentPageValue;
    int reportTotalPages = reportTotalPagesValue == null ? 1 : reportTotalPagesValue;
    int userCurrentPage = userCurrentPageValue == null ? 1 : userCurrentPageValue;
    int userTotalPages = userTotalPagesValue == null ? 1 : userTotalPagesValue;
    String reportPageUrlPrefix = request.getContextPath() + "/admin?userPage=" + userCurrentPage + "&reportPage=";
    String userPageUrlPrefix = request.getContextPath() + "/admin?reportPage=" + reportCurrentPage + "&userPage=";
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
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main class="page">
    <section class="page-heading">
        <div>
            <h1>管理员后台</h1>
        </div>
    </section>

    <section class="admin-section" data-admin-section="reports">
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
                        <form method="post" action="<%= ctx %>/admin/action" class="report-handle-form ajax-admin-action">
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
            <%= renderPagination(reportPageUrlPrefix, reportCurrentPage, reportTotalPages, "举报分页") %>
        <% } %>
    </section>

    <section class="admin-section" data-admin-section="users">
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
                    <% String userFormId = "admin-user-form-" + item.getId(); %>
                    <tr>
                        <td><%= TextUtils.escapeHtml(item.getUsername()) %></td>
                        <td><input form="<%= userFormId %>" type="text" name="phone" value="<%= TextUtils.escapeHtml(item.getPhone()) %>"></td>
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
                            <select form="<%= userFormId %>" name="banAction" <%= item.getId() == loginUser.getId() ? "disabled" : "" %>>
                                <option value="0:0" <%= "0:0".equals(banState) ? "selected" : "" %>>正常</option>
                                <option value="1:1" <%= "1:1".equals(banState) ? "selected" : "" %>>封禁 1 天</option>
                                <option value="1:7" <%= "1:7".equals(banState) ? "selected" : "" %>>封禁 7 天</option>
                                <option value="1:30" <%= "1:30".equals(banState) ? "selected" : "" %>>封禁 30 天</option>
                                <option value="1:365" <%= "1:365".equals(banState) ? "selected" : "" %>>封禁 365 天</option>
                                <option value="1:0" <%= "1:0".equals(banState) ? "selected" : "" %>>永久封禁</option>
                            </select>
                            <% if (item.getId() == loginUser.getId()) { %>
                                <input form="<%= userFormId %>" type="hidden" name="banAction" value="0:0">
                            <% } %>
                            <input form="<%= userFormId %>" type="hidden" name="role" value="<%= item.getRole() %>">
                        </td>
                        <td>
                            <form id="<%= userFormId %>" method="post" action="<%= ctx %>/admin/action" class="ajax-admin-action">
                                <input type="hidden" name="action" value="user">
                                <input type="hidden" name="userId" value="<%= item.getId() %>">
                            </form>
                            <button form="<%= userFormId %>" class="button" type="submit">保存</button>
                        </td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </div>
        <%= renderPagination(userPageUrlPrefix, userCurrentPage, userTotalPages, "用户分页") %>
    </section>
    <div class="inline-message" id="actionMessage" aria-live="polite"></div>
</main></body>
</html>
<%!
    private String renderPagination(String pageUrlPrefix, int currentPage, int totalPages, String label) {
        if (totalPages <= 1) {
            return "";
        }
        StringBuilder html = new StringBuilder("<nav class=\"pagination\" aria-label=\"").append(label).append("\">");
        if (currentPage > 1) {
            html.append("<a class=\"page-link\" href=\"").append(pageUrlPrefix).append(currentPage - 1).append("\">上一页</a>");
        } else {
            html.append("<span class=\"page-link disabled\">上一页</span>");
        }
        for (int i = 1; i <= totalPages; i++) {
            if (i == currentPage) {
                html.append("<span class=\"page-link active\">").append(i).append("</span>");
            } else {
                html.append("<a class=\"page-link\" href=\"").append(pageUrlPrefix).append(i).append("\">").append(i).append("</a>");
            }
        }
        if (currentPage < totalPages) {
            html.append("<a class=\"page-link\" href=\"").append(pageUrlPrefix).append(currentPage + 1).append("\">下一页</a>");
        } else {
            html.append("<span class=\"page-link disabled\">下一页</span>");
        }
        html.append("</nav>");
        return html.toString();
    }
%>
