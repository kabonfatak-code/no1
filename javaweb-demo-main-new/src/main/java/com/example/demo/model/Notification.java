package com.example.demo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Notification implements Serializable {
    private long id;
    private long postId;
    private long commentId;
    private String type;
    private String actorUsername;
    private String postTitle;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public Notification() {
    }

    public Notification(long id, String type, String actorUsername, String postTitle, String message, boolean read, LocalDateTime createdAt) {
        this(id, 0L, 0L, type, actorUsername, postTitle, message, read, createdAt);
    }

    public Notification(long id, long postId, long commentId, String type, String actorUsername, String postTitle, String message, boolean read, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.commentId = commentId;
        this.type = type;
        this.actorUsername = actorUsername;
        this.postTitle = postTitle;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getPostId() {
        return postId;
    }

    public long getCommentId() {
        return commentId;
    }

    public String getType() {
        return type;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getTargetUrl(String contextPath) {
        if (postId <= 0) {
            return contextPath + "/notifications";
        }
        String url = contextPath + "/post/detail?id=" + postId;
        if (commentId > 0) {
            url += "#comment-" + commentId;
        }
        return url;
    }
}
