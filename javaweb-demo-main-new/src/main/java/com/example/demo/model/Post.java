package com.example.demo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Post implements Serializable {
    private long id;
    private String title;
    private String topic;
    private String region;
    private String content;
    private long authorId;
    private String authorUsername;
    private String authorRole;
    private String authorIp;
    private boolean pinned;
    private boolean deleted;
    private int likeScore;
    private int dislikeScore;
    private int favoriteCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Post() {
    }

    public Post(long id, String title, String topic, String region, String content, long authorId, String authorUsername,
                String authorRole, String authorIp, boolean pinned, boolean deleted, int likeScore, int dislikeScore,
                int favoriteCount, int commentCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.topic = topic;
        this.region = region;
        this.content = content;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.authorRole = authorRole;
        this.authorIp = authorIp;
        this.pinned = pinned;
        this.deleted = deleted;
        this.likeScore = likeScore;
        this.dislikeScore = dislikeScore;
        this.favoriteCount = favoriteCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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

    public String getAuthorRole() {
        return authorRole;
    }

    public void setAuthorRole(String authorRole) {
        this.authorRole = authorRole;
    }

    public String getAuthorIp() {
        return authorIp;
    }

    public void setAuthorIp(String authorIp) {
        this.authorIp = authorIp;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
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

    public int getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(int favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
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
