<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.time.format.DateTimeFormatter,java.util.ArrayList,java.util.HashMap,java.util.LinkedHashMap,java.util.List,java.util.Map,com.example.demo.model.Post,com.example.demo.model.Comment,com.example.demo.util.TextUtils,com.example.demo.util.ForumOptions" %>
<%
    Post post = (Post) request.getAttribute("post");
    List<Comment> comments = (List<Comment>) request.getAttribute("comments");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    Map<Long, Comment> commentsById = new HashMap<>();
    List<Comment> topComments = new ArrayList<>();
    Map<Long, List<Comment>> repliesByParent = new LinkedHashMap<>();
    if (comments != null) {
        for (Comment comment : comments) {
            commentsById.put(comment.getId(), comment);
        }
        for (Comment comment : comments) {
            long parentId = comment.getParentCommentId();
            if (parentId <= 0 || !commentsById.containsKey(parentId)) {
                topComments.add(comment);
            } else {
                long rootParentId = parentId;
                Comment parent = commentsById.get(rootParentId);
                while (parent != null && parent.getParentCommentId() > 0 && commentsById.containsKey(parent.getParentCommentId())) {
                    rootParentId = parent.getParentCommentId();
                    parent = commentsById.get(rootParentId);
                }
                repliesByParent.computeIfAbsent(rootParentId, ignored -> new ArrayList<>()).add(comment);
            }
        }
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>留言详情 - BBS 论坛</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css?v=20260627-reply-actions">
</head>
<body>
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main class="page">
    <article class="detail">
        <div class="detail-head">
            <div>
                <div class="tag-row">
                    <% if (post.isPinned()) { %><span class="tag pin">置顶</span><% } %>
                    <span class="tag"><%= TextUtils.escapeHtml(post.getTopic()) %></span>
                    <span class="tag"><%= TextUtils.escapeHtml(post.getRegion()) %></span>
                </div>
                <p>作者：<%= TextUtils.escapeHtml(post.getAuthorUsername()) %> · <%= TextUtils.escapeHtml(ForumOptions.roleLabel(post.getAuthorRole())) %> · <%= formatter.format(post.getCreatedAt()) %></p>
            </div>
            <% if (loginUser != null && (loginUser.isAdmin() || loginUser.getId() == post.getAuthorId())) { %>
                <div class="button-row">
                    <a class="button" href="<%= ctx %>/post/edit?id=<%= post.getId() %>">编辑</a>
                    <form method="post" action="<%= ctx %>/post/action" onsubmit="return confirm('确认删除该留言？');">
                        <input type="hidden" name="id" value="<%= post.getId() %>">
                        <input type="hidden" name="action" value="delete">
                        <button class="button danger" type="submit">删除</button>
                    </form>
                </div>
            <% } %>
        </div>
        <div class="detail-content"><%= TextUtils.escapeHtmlWithLineBreaks(post.getContent()) %></div>
        <div class="stats action-stats" id="actions" data-post-stats data-post-id="<%= post.getId() %>">
            <span>赞 <span data-post-stat="likeScore"><%= post.getLikeScore() %></span></span>
            <span>踩 <span data-post-stat="dislikeScore"><%= post.getDislikeScore() %></span></span>
            <span>收藏 <span data-post-stat="favoriteCount"><%= post.getFavoriteCount() %></span></span>
            <span>评论 <span data-post-stat="commentCount"><%= post.getCommentCount() %></span></span>
        </div>

        <% if (loginUser != null) { %>
            <div class="action-panel">
                <form method="post" action="<%= ctx %>/post/action" class="ajax-action post-action-buttons">
                    <input type="hidden" name="id" value="<%= post.getId() %>">
                    <button class="button" type="submit" name="action" value="like">赞</button>
                    <button class="button" type="submit" name="action" value="dislike">踩</button>
                    <button class="button" type="submit" name="action" value="favorite">收藏</button>
                </form>
                <% if (!loginUser.isAdmin()) { %>
                    <form method="post" action="<%= ctx %>/post/action" class="report-form ajax-action" data-disclosure-form autocomplete="off">
                        <input type="hidden" name="id" value="<%= post.getId() %>">
                        <input type="hidden" name="action" value="report">
                        <button class="button" type="button" data-disclosure-toggle>举报</button>
                        <div class="report-fields hidden" data-disclosure-fields>
                            <input type="text" name="reason" placeholder="举报原因" autocomplete="off" required>
                            <button class="button" type="submit">提交</button>
                        </div>
                    </form>
                <% } %>
            </div>
        <% } %>
    </article>

    <section class="comments" id="comments">
        <h2>评论</h2>
        <% if (loginUser != null) { %>
            <form method="post" action="<%= ctx %>/comment/action" class="comment-form ajax-action" autocomplete="off">
                <input type="hidden" name="action" value="add">
                <input type="hidden" name="postId" value="<%= post.getId() %>">
                <input type="hidden" name="parentCommentId" value="0">
                <textarea name="content" rows="4" maxlength="2000" placeholder="写下你的评论" autocomplete="off" required></textarea>
                <button class="button primary" type="submit">发表评论</button>
            </form>
        <% } %>

        <% if (topComments.isEmpty()) { %>
            <div class="empty-state small">暂无评论</div>
        <% } else { %>
            <% for (Comment comment : topComments) {
                List<Comment> replies = repliesByParent.get(comment.getId());
                int replyCount = replies == null ? 0 : replies.size();
            %>
                <article class="comment" id="comment-<%= comment.getId() %>">
                    <div class="comment-meta">
                        <strong><%= TextUtils.escapeHtml(comment.getAuthorUsername()) %></strong>
                        <time><%= formatter.format(comment.getCreatedAt()) %></time>
                        <span class="comment-region"><%= TextUtils.escapeHtml(TextUtils.trim(comment.getRegion()).isEmpty() ? "未知" : comment.getRegion()) %></span>
                        <span class="comment-stats" data-comment-stats data-comment-id="<%= comment.getId() %>">
                            赞 <span data-comment-stat="likeScore"><%= comment.getLikeScore() %></span> ·
                            踩 <span data-comment-stat="dislikeScore"><%= comment.getDislikeScore() %></span>
                        </span>
                    </div>
                    <div class="comment-content"><%= TextUtils.escapeHtmlWithLineBreaks(comment.getContent()) %></div>
                    <% if (loginUser != null) { %>
                        <div class="button-row compact-buttons">
                            <form method="post" action="<%= ctx %>/comment/action" class="ajax-action">
                                <input type="hidden" name="postId" value="<%= post.getId() %>">
                                <input type="hidden" name="commentId" value="<%= comment.getId() %>">
                                <button class="button" type="submit" name="action" value="like">赞</button>
                                <button class="button" type="submit" name="action" value="dislike">踩</button>
                            </form>
                            <form method="post" action="<%= ctx %>/comment/action" class="comment-reply ajax-action" data-disclosure-form autocomplete="off">
                                <input type="hidden" name="action" value="add">
                                <input type="hidden" name="postId" value="<%= post.getId() %>">
                                <input type="hidden" name="parentCommentId" value="<%= comment.getId() %>">
                                <button class="button" type="button" data-disclosure-toggle>回复</button>
                                <div class="comment-reply-fields hidden" data-disclosure-fields>
                                    <textarea name="content" rows="2" maxlength="2000" placeholder="回复 @<%= TextUtils.escapeHtml(comment.getAuthorUsername()) %>" autocomplete="off" required></textarea>
                                    <div class="button-row">
                                        <button class="button" type="submit">提交回复</button>
                                        <button class="button" type="button" data-disclosure-cancel>取消</button>
                                    </div>
                                </div>
                            </form>
                            <% if (!loginUser.isAdmin()) { %>
                                <form method="post" action="<%= ctx %>/comment/action" class="report-form ajax-action" data-disclosure-form autocomplete="off">
                                    <input type="hidden" name="postId" value="<%= post.getId() %>">
                                    <input type="hidden" name="commentId" value="<%= comment.getId() %>">
                                    <input type="hidden" name="action" value="report">
                                    <button class="button" type="button" data-disclosure-toggle>举报</button>
                                    <div class="report-fields hidden" data-disclosure-fields>
                                        <input type="text" name="reason" placeholder="举报评论原因" autocomplete="off" required>
                                        <button class="button" type="submit">提交</button>
                                    </div>
                                </form>
                            <% } %>
                        </div>
                        <% if (loginUser.isAdmin() || loginUser.getId() == comment.getAuthorId()) { %>
                            <form method="post" action="<%= ctx %>/comment/action" class="comment-edit ajax-action" data-disclosure-form autocomplete="off">
                                <input type="hidden" name="postId" value="<%= post.getId() %>">
                                <input type="hidden" name="commentId" value="<%= comment.getId() %>">
                                <div class="button-row comment-edit-actions">
                                    <button class="button" type="button" data-disclosure-toggle>编辑</button>
                                    <button class="button danger" type="submit" name="action" value="delete" onclick="return confirm('确认删除该评论？');">删除评论</button>
                                </div>
                                <div class="comment-edit-fields hidden" data-disclosure-fields>
                                    <textarea name="content" rows="3" autocomplete="off"><%= TextUtils.escapeHtml(comment.getContent()) %></textarea>
                                    <div class="button-row">
                                        <button class="button" type="submit" name="action" value="edit">保存</button>
                                        <button class="button" type="button" data-disclosure-cancel>取消</button>
                                    </div>
                                </div>
                            </form>
                        <% } %>
                    <% } %>
                    <% if (replyCount > 0) { %>
                        <button class="reply-list-toggle" type="button"
                                data-reply-list-toggle
                                data-target="replies-for-<%= comment.getId() %>"
                                data-closed-text="展开 <%= replyCount %> 条回复"
                                data-open-text="收起回复"
                                aria-expanded="false">
                            展开 <%= replyCount %> 条回复
                        </button>
                        <div class="reply-list collapsed" id="replies-for-<%= comment.getId() %>" data-reply-list>
                            <% for (Comment reply : replies) { %>
                                <article class="comment reply-comment" id="comment-<%= reply.getId() %>">
                                    <div class="comment-meta">
                                        <strong><%= TextUtils.escapeHtml(reply.getAuthorUsername()) %></strong>
                                        <% if (!TextUtils.trim(reply.getParentAuthorUsername()).isEmpty()) { %>
                                            <span class="reply-tag">回复 @<%= TextUtils.escapeHtml(reply.getParentAuthorUsername()) %></span>
                                        <% } %>
                                        <time><%= formatter.format(reply.getCreatedAt()) %></time>
                                        <span class="comment-region"><%= TextUtils.escapeHtml(TextUtils.trim(reply.getRegion()).isEmpty() ? "未知" : reply.getRegion()) %></span>
                                        <span class="comment-stats" data-comment-stats data-comment-id="<%= reply.getId() %>">
                                            赞 <span data-comment-stat="likeScore"><%= reply.getLikeScore() %></span> ·
                                            踩 <span data-comment-stat="dislikeScore"><%= reply.getDislikeScore() %></span>
                                        </span>
                                    </div>
                                    <div class="comment-content"><%= TextUtils.escapeHtmlWithLineBreaks(reply.getContent()) %></div>
                                    <% if (loginUser != null) { %>
                                        <div class="button-row compact-buttons">
                                            <form method="post" action="<%= ctx %>/comment/action" class="ajax-action">
                                                <input type="hidden" name="postId" value="<%= post.getId() %>">
                                                <input type="hidden" name="commentId" value="<%= reply.getId() %>">
                                                <button class="button" type="submit" name="action" value="like">赞</button>
                                                <button class="button" type="submit" name="action" value="dislike">踩</button>
                                            </form>
                                            <form method="post" action="<%= ctx %>/comment/action" class="comment-reply ajax-action" data-disclosure-form autocomplete="off">
                                                <input type="hidden" name="action" value="add">
                                                <input type="hidden" name="postId" value="<%= post.getId() %>">
                                                <input type="hidden" name="parentCommentId" value="<%= reply.getId() %>">
                                                <button class="button" type="button" data-disclosure-toggle>回复</button>
                                                <div class="comment-reply-fields hidden" data-disclosure-fields>
                                                    <textarea name="content" rows="2" maxlength="2000" placeholder="回复 @<%= TextUtils.escapeHtml(reply.getAuthorUsername()) %>" autocomplete="off" required></textarea>
                                                    <div class="button-row">
                                                        <button class="button" type="submit">提交回复</button>
                                                        <button class="button" type="button" data-disclosure-cancel>取消</button>
                                                    </div>
                                                </div>
                                            </form>
                                            <% if (!loginUser.isAdmin()) { %>
                                                <form method="post" action="<%= ctx %>/comment/action" class="report-form ajax-action" data-disclosure-form autocomplete="off">
                                                    <input type="hidden" name="postId" value="<%= post.getId() %>">
                                                    <input type="hidden" name="commentId" value="<%= reply.getId() %>">
                                                    <input type="hidden" name="action" value="report">
                                                    <button class="button" type="button" data-disclosure-toggle>举报</button>
                                                    <div class="report-fields hidden" data-disclosure-fields>
                                                        <input type="text" name="reason" placeholder="举报评论原因" autocomplete="off" required>
                                                        <button class="button" type="submit">提交</button>
                                                    </div>
                                                </form>
                                            <% } %>
                                        </div>
                                        <% if (loginUser.isAdmin() || loginUser.getId() == reply.getAuthorId()) { %>
                                            <form method="post" action="<%= ctx %>/comment/action" class="comment-edit ajax-action" data-disclosure-form autocomplete="off">
                                                <input type="hidden" name="postId" value="<%= post.getId() %>">
                                                <input type="hidden" name="commentId" value="<%= reply.getId() %>">
                                                <div class="button-row comment-edit-actions">
                                                    <button class="button" type="button" data-disclosure-toggle>编辑</button>
                                                    <button class="button danger" type="submit" name="action" value="delete" onclick="return confirm('确认删除该评论？');">删除评论</button>
                                                </div>
                                                <div class="comment-edit-fields hidden" data-disclosure-fields>
                                                    <textarea name="content" rows="3" autocomplete="off"><%= TextUtils.escapeHtml(reply.getContent()) %></textarea>
                                                    <div class="button-row">
                                                        <button class="button" type="submit" name="action" value="edit">保存</button>
                                                        <button class="button" type="button" data-disclosure-cancel>取消</button>
                                                    </div>
                                                </div>
                                            </form>
                                        <% } %>
                                    <% } %>
                                </article>
                            <% } %>
                        </div>
                    <% } %>
                </article>
            <% } %>
        <% } %>
    </section>
    <a class="back-link" href="<%= ctx %>/posts">返回主页</a>
    <div class="inline-message" id="actionMessage" aria-live="polite"></div>
</main>
</body>
</html>
