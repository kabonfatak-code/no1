package com.example.demo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Comment implements Serializable {
    private long id;
    private long postId;
    private long parentCommentId;
    private long authorId;
    private String authorUsername;
    private String parentAuthorUsername;
    private String ipAddress;
    private String region;
    private String content;
    private int likeScore;
    private int dislikeScore;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Comment() {
    }

    public Comment(long id, long postId, long parentCommentId, long authorId, String authorUsername,
                   String parentAuthorUsername, String ipAddress, String region, String content,
                   int likeScore, int dislikeScore, boolean deleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.postId = postId;
        this.parentCommentId = parentCommentId;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.parentAuthorUsername = parentAuthorUsername;
        this.ipAddress = ipAddress;
        this.region = region;
        this.content = content;
        this.likeScore = likeScore;
        this.dislikeScore = dislikeScore;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public long getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(long parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public String getParentAuthorUsername() {
        return parentAuthorUsername;
    }

    public void setParentAuthorUsername(String parentAuthorUsername) {
        this.parentAuthorUsername = parentAuthorUsername;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getLikeScore() {
        return likeScore;
    }

    public void setLikeScore(int likeScore) {
        this.likeScore = likeScore;
    }

    public int getDislikeScore() {
        return dislikeScore;
    }

    public void setDislikeScore(int dislikeScore) {
        this.dislikeScore = dislikeScore;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
