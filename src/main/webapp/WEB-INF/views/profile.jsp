<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.time.format.DateTimeFormatter,java.util.List,com.example.demo.model.Post,com.example.demo.model.User,com.example.demo.util.TextUtils,com.example.demo.util.WebUtil" %>
<%
    User currentUser = (User) session.getAttribute(WebUtil.LOGIN_USER_KEY);
    String tab = (String) request.getAttribute("tab");
    if (tab == null || tab.isEmpty()) {
        tab = "account";
    }
    List<Post> history = (List<Post>) request.getAttribute("history");
    List<Post> favorites = (List<Post>) request.getAttribute("favorites");
    List<Post> myPosts = (List<Post>) request.getAttribute("myPosts");
    String error = (String) request.getAttribute("error");
    String editUsername = (String) request.getAttribute("editUsername");
    String editPhone = (String) request.getAttribute("editPhone");
    if (editUsername == null) {
        editUsername = currentUser == null ? "" : currentUser.getUsername();
    }
    if (editPhone == null) {
        editPhone = currentUser == null ? "" : currentUser.getPhone();
    }
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>个人主页 - BBS 论坛</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/fragments/header-v2.jspf" %>
<main class="page">
    <section class="profile-shell">
        <nav class="tabs">
            <a class="<%= "account".equals(tab) ? "active" : "" %>" href="<%= ctx %>/profile?tab=account">账号信息</a>
            <a class="<%= "edit".equals(tab) ? "active" : "" %>" href="<%= ctx %>/profile?tab=edit">账号修改</a>
            <a class="<%= "history".equals(tab) ? "active" : "" %>" href="<%= ctx %>/profile?tab=history">历史浏览</a>
            <a class="<%= "favorites".equals(tab) ? "active" : "" %>" href="<%= ctx %>/profile?tab=favorites">收藏</a>
            <a class="<%= "posts".equals(tab) ? "active" : "" %>" href="<%= ctx %>/profile?tab=posts">我的留言</a>
        </nav>

        <% if ("account".equals(tab)) { %>
            <section class="form-card flat">
                <h1>账号信息</h1>
                <div class="info-grid">
                    <span>用户名</span><strong><%= TextUtils.escapeHtml(loginUser.getUsername()) %></strong>
                    <span>电话</span><strong><%= TextUtils.escapeHtml(loginUser.getPhone()) %></strong>
                    <span>用户类型</span><strong><%= TextUtils.escapeHtml(loginUser.getRoleLabel()) %></strong>
                    <span>历史记录</span><strong><%= loginUser.isHistoryEnabled() ? "开启" : "关闭" %></strong>
                </div>
                <form method="post" action="<%= ctx %>/profile">
                    <input type="hidden" name="action" value="history">
                    <label class="checkline">
                        <input type="checkbox" name="historyEnabled" <%= loginUser.isHistoryEnabled() ? "checked" : "" %>>
                        记录历史浏览
                    </label>
                    <button class="button primary" type="submit">保存设置</button>
                </form>
            </section>
        <% } else if ("edit".equals(tab)) { %>
            <section class="form-card flat">
                <h1>账号修改</h1>
                <% if (error != null) { %><div class="error"><%= TextUtils.escapeHtml(error) %></div><% } %>
                <form method="post" action="<%= ctx %>/profile" class="account-edit-form">
                    <input type="hidden" name="action" value="account">
                    <label>用户名
                        <input type="text" name="username" value="<%= TextUtils.escapeHtml(editUsername) %>" maxlength="20" required>
                    </label>
                    <label>手机号
                        <input type="text" name="phone" value="<%= TextUtils.escapeHtml(editPhone) %>" maxlength="20" required>
                    </label>
                    <label>新密码
                        <input type="password" name="password" maxlength="32" minlength="6" placeholder="不修改可留空">
                    </label>
                    <button class="button primary" type="submit">保存修改</button>
                </form>
            </section>
        <% } else if ("history".equals(tab)) { %>
            <%= renderPosts(history, ctx, formatter) %>
        <% } else if ("favorites".equals(tab)) { %>
            <%= renderPosts(favorites, ctx, formatter) %>
        <% } else { %>
            <% if (myPosts == null || myPosts.isEmpty()) { %>
                <section class="empty-state">你还没有发表留言</section>
            <% } else { %>
                <section class="post-list">
                    <% for (Post post : myPosts) { %>
                        <article class="post-card">
                            <div class="tag-row">
                                <% if (post.isPinned()) { %><span class="tag pin">置顶</span><% } %>
                                <span class="tag"><%= TextUtils.escapeHtml(post.getTopic()) %></span>
                                <span class="tag"><%= TextUtils.escapeHtml(post.getRegion()) %></span>
                            </div>
                            <a class="post-content-link" href="<%= ctx %>/post/detail?id=<%= post.getId() %>"><%= excerpt(post.getContent()) %></a>
                            <div class="post-meta"><%= formatter.format(post.getCreatedAt()) %> · 赞 <%= post.getLikeScore() %> · 收藏 <%= post.getFavoriteCount() %></div>
                            <div class="button-row">
                                <a class="button" href="<%= ctx %>/post/edit?id=<%= post.getId() %>">编辑</a>
                                <form method="post" action="<%= ctx %>/post/action" onsubmit="return confirm('确认删除该留言？');">
                                    <input type="hidden" name="id" value="<%= post.getId() %>">
                                    <button class="button danger" type="submit" name="action" value="delete">删除</button>
                                </form>
                            </div>
                        </article>
                    <% } %>
                </section>
            <% } %>
        <% } %>
    </section>
</main>
</body>
</html>
<%!
    private String renderPosts(List<Post> posts, String ctx, DateTimeFormatter formatter) {
        if (posts == null || posts.isEmpty()) {
            return "<section class=\"empty-state\">暂无记录</section>";
        }
        StringBuilder html = new StringBuilder("<section class=\"post-list\">");
        for (Post post : posts) {
            html.append("<article class=\"post-card\">")
                    .append("<div class=\"tag-row\">");
            if (post.isPinned()) {
                html.append("<span class=\"tag pin\">置顶</span>");
            }
            html.append("<span class=\"tag\">").append(TextUtils.escapeHtml(post.getTopic())).append("</span>")
                    .append("<span class=\"tag\">").append(TextUtils.escapeHtml(post.getRegion())).append("</span>")
                    .append("</div>")
                    .append("<a class=\"post-content-link\" href=\"").append(ctx).append("/post/detail?id=").append(post.getId()).append("\">")
                    .append(excerpt(post.getContent())).append("</a>")
                    .append("<div class=\"post-meta\">").append(formatter.format(post.getCreatedAt()))
                    .append(" · 赞 ").append(post.getLikeScore())
                    .append(" · 收藏 ").append(post.getFavoriteCount())
                    .append("</div></article>");
        }
        html.append("</section>");
        return html.toString();
    }

    private String excerpt(String content) {
        String text = content == null ? "" : content;
        return TextUtils.escapeHtml(text.length() > 120 ? text.substring(0, 120) + "..." : text);
    }
%>
