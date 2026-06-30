package com.example.demo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Report implements Serializable {
    private long id;
    private long postId;
    private long commentId;
    private long targetUserId;
    private String postTitle;
    private String targetType;
    private String targetUsername;
    private String reporterUsername;
    private String reason;
    private int weight;
    private int reportCount;
    private boolean handled;
    private LocalDateTime createdAt;

    public Report() {
    }

    public Report(long id, long postId, String postTitle, String reporterUsername, String reason, int weight, boolean handled, LocalDateTime createdAt) {
        this(id, postId, 0L, 0L, postTitle, postId > 0 ? "post" : "comment", "", reporterUsername, reason, weight, weight, handled, createdAt);
    }

    public Report(long id, long postId, long commentId, long targetUserId, String postTitle, String targetType,
                  String targetUsername, String reporterUsername, String reason, int weight, int reportCount,
                  boolean handled, LocalDateTime createdAt) {
        this.id = id;
        this.postId = postId;
        this.commentId = commentId;
        this.targetUserId = targetUserId;
        this.postTitle = postTitle;
        this.targetType = targetType;
        this.targetUsername = targetUsername;
        this.reporterUsername = reporterUsername;
        this.reason = reason;
        this.weight = weight;
        this.reportCount = reportCount;
        this.handled = handled;
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

    public long getTargetUserId() {
        return targetUserId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public String getReporterUsername() {
        return reporterUsername;
    }

    public String getReason() {
        return reason;
    }

    public int getWeight() {
        return weight;
    }

    public int getReportCount() {
        return reportCount;
    }

    public String getTargetAnchor() {
        if ("comment".equals(targetType) && commentId > 0) {
            return "#comment-" + commentId;
        }
        return "";
    }

    public boolean isHandled() {
        return handled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
