package com.example.demo.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class User implements Serializable {
    public static final String ROLE_NEW = "NEW_USER";
    public static final String ROLE_OLD = "OLD_USER";
    public static final String ROLE_ADMIN = "ADMIN";

    private long id;
    private String username;
    private String passwordHash;
    private String phone;
    private String province;
    private String role;
    private boolean banned;
    private LocalDateTime bannedUntil;
    private boolean historyEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime registerTime;

    public User() {
    }

    public User(long id, String username, String passwordHash, String phone, String role, boolean banned, boolean historyEnabled, LocalDateTime createdAt) {
        this(id, username, passwordHash, phone, "", role, banned, null, historyEnabled, createdAt, createdAt);
    }

    public User(long id, String username, String passwordHash, String phone, String province, String role, boolean banned,
                LocalDateTime bannedUntil, boolean historyEnabled, LocalDateTime createdAt, LocalDateTime registerTime) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.province = province;
        this.role = role;
        this.banned = banned;
        this.bannedUntil = bannedUntil;
        this.historyEnabled = historyEnabled;
        this.createdAt = createdAt;
        this.registerTime = registerTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public boolean isOldUser() {
        LocalDateTime baseTime = registerTime == null ? createdAt : registerTime;
        return !isAdmin() && baseTime != null && !baseTime.isAfter(LocalDateTime.now().minusMonths(6));
    }

    public boolean isBanned() {
        return banned && (bannedUntil == null || bannedUntil.isAfter(LocalDateTime.now()));
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public LocalDateTime getBannedUntil() {
        return bannedUntil;
    }

    public void setBannedUntil(LocalDateTime bannedUntil) {
        this.bannedUntil = bannedUntil;
    }

    public boolean isHistoryEnabled() {
        return historyEnabled;
    }

    public void setHistoryEnabled(boolean historyEnabled) {
        this.historyEnabled = historyEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRegisterTime() {
        return registerTime == null ? createdAt : registerTime;
    }

    public void setRegisterTime(LocalDateTime registerTime) {
        this.registerTime = registerTime;
    }

    public String getRoleLabel() {
        if (ROLE_ADMIN.equals(role)) {
            return "系统管理员";
        }
        if (isOldUser()) {
            return "老东西";
        }
        return "新用户";
    }
}
