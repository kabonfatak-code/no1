package com.example.demo.model;

import java.io.Serializable;

public class User implements Serializable {
    private String username;
    private String passwordHash;
    private String realName;
    private String email;
    private String gender;
    private String phone;
    private boolean admin;

    public User() {
    }

    public User(String username, String passwordHash, String realName, String email, String gender, String phone, boolean admin) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.realName = realName;
        this.email = email;
        this.gender = gender;
        this.phone = phone;
        this.admin = admin;
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

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
