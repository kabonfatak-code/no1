<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.time.format.DateTimeFormatter,java.net.URLEncoder,java.io.UnsupportedEncodingException,java.util.List,com.example.demo.model.Post,com.example.demo.model.PostSearchCriteria,com.example.demo.util.TextUtils,com.example.demo.util.ForumOptions" %>
<%!
    private String encodeQuery(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
%>
<%
    List<Post> posts = (List<Post>) request.getAttribute("posts");
    List<String> topics = (List<String>) request.getAttribute("topics");
    PostSearchCriteria criteria = (PostSearchCriteria) request.getAttribute("criteria");
    Integer totalCount = (Integer) request.getAttribute("totalCount");
    Integer currentPageValue = (Integer) request.getAttribute("currentPage");
    Integer totalPagesValue = (Integer) request.getAttribute("totalPages");
    int currentPage = currentPageValue == null ? 1 : currentPageValue;
    int totalPages = totalPagesValue == null ? 1 : totalPagesValue;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    String orderBy = criteria == null ? "" : TextUtils.trim(criteria.getOrderBy());
    StringBuilder pageQuery = new StringBuilder(request.getContextPath()).append("/posts?");
    if (!TextUtils.trim(criteria.getTopic()).isEmpty()) {
        pageQuery.append("topic=").append(encodeQuery(criteria.getTopic())).append("&");
    }
    if (!TextUtils.trim(criteria.getKeyword()).isEmpty()) {
        pageQuery.append("keyword=").append(encodeQuery(criteria.getKeyword())).append("&");
    }
    if (criteria.getDays() > 0) {
        pageQuery.append("days=").append(criteria.getDays()).append("&");
    }
    if (criteria.getMinLikes() > 0) {
        pageQuery.append("minLikes=").append(criteria.getMinLikes()).append("&");
    }
    if (criteria.getMinFavorites() > 0) {
        pageQuery.append("minFavorites=").append(criteria.getMinFavorites()).append("&");
    }
    if (!orderBy.isEmpty()) {
        pageQuery.append("orderBy=").append(encodeQuery(orderBy)).append("&");
    }
    String pageUrlPrefix = pageQuery.append("page=").toString();
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>主页 - BBS 论坛</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main class="layout">
    <aside class="sidebar">
        <h2>检索</h2>
        <form method="get" action="<%= ctx %>/posts" class="filter-form">
            <label>主题
                <select name="topic">
                    <option value="">全部已发主题</option>
                    <% if (topics != null) { for (String topic : topics) { %>
                        <option value="<%= TextUtils.escapeHtml(topic) %>" <%= topic.equals(criteria.getTopic()) ? "selected" : "" %>><%= TextUtils.escapeHtml(topic) %></option>
                    <% }} %>
                </select>
            </label>
            <label>关键词
                <input type="text" name="keyword" value="<%= TextUtils.escapeHtml(criteria.getKeyword()) %>" placeholder="正文关键词">
            </label>
            <label>时间
                <select name="days">
                    <option value="0">不限</option>
                    <option value="1" <%= criteria.getDays() == 1 ? "selected" : "" %>>1 天内</option>
                    <option value="7" <%= criteria.getDays() == 7 ? "selected" : "" %>>7 天内</option>
                    <option value="30" <%= criteria.getDays() == 30 ? "selected" : "" %>>30 天内</option>
                </select>
            </label>
            <label>最低点赞量
                <input type="number" min="0" name="minLikes" value="<%= criteria.getMinLikes() %>">
            </label>
            <label>最低收藏量
                <input type="number" min="0" name="minFavorites" value="<%= criteria.getMinFavorites() %>">
            </label>
            <label>排序
                <select name="orderBy">
                    <option value="" <%= orderBy.isEmpty() ? "selected" : "" %>>默认：点赞量</option>
                    <option value="favorites" <%= "favorites".equals(orderBy) ? "selected" : "" %>>收藏量</option>
                    <option value="time" <%= "time".equals(orderBy) ? "selected" : "" %>>发布时间</option>
                </select>
            </label>
            <button class="button primary full" type="submit">搜索</button>
            <a class="button full" href="<%= ctx %>/posts">清空</a>
        </form>
    </aside>

    <section class="feed">
        <div class="page-heading compact">
            <div>
                <h1>留言</h1>
            </div>
            <% if (loginUser != null) { %>
                <a class="button primary" href="<%= ctx %>/post/new">发表留言</a>
            <% } %>
        </div>

        <% if (posts == null || posts.isEmpty()) { %>
            <section class="empty-state">暂无留言</section>
        <% } else { %>
            <div class="post-list">
                <% for (Post post : posts) { %>
                    <article class="post-card">
                        <div class="post-card-head">
                            <div>
                                <div class="tag-row">
                                    <% if (post.isPinned()) { %><span class="tag pin">置顶</span><% } %>
                                    <span class="tag"><%= TextUtils.escapeHtml(post.getTopic()) %></span>
                                    <span class="tag"><%= TextUtils.escapeHtml(post.getRegion()) %></span>
                                </div>
                                <a class="post-content-link" href="<%= ctx %>/post/detail?id=<%= post.getId() %>"><%= TextUtils.escapeHtml(post.getContent().length() > 120 ? post.getContent().substring(0, 120) + "..." : post.getContent()) %></a>
                                <div class="post-meta">
                                    <span><%= TextUtils.escapeHtml(post.getAuthorUsername()) %> · <%= TextUtils.escapeHtml(ForumOptions.roleLabel(post.getAuthorRole())) %></span>
                                    <time><%= formatter.format(post.getCreatedAt()) %></time>
                                </div>
                            </div>
                        </div>
                        <div class="stats">
                            <span>赞 <%= post.getLikeScore() %></span>
                            <span>踩 <%= post.getDislikeScore() %></span>
                            <span>收藏 <%= post.getFavoriteCount() %></span>
                            <span>评论 <%= post.getCommentCount() %></span>
                        </div>
                    </article>
                <% } %>
            </div>
            <% if (totalPages > 1) { %>
                <nav class="pagination" aria-label="留言分页">
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
        <% } %>
    </section>
</main>
</body>
</html>
