package com.example.demo.repository;

import com.example.demo.model.Post;
import com.example.demo.model.User;
import com.example.demo.util.PasswordUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BbsRepository {
    private final Map<String, User> users = new LinkedHashMap<>();
    private final List<Post> posts = new ArrayList<>();
    private long nextPostId = 1L;

    public BbsRepository() {
        User admin = new User(
                "admin",
                PasswordUtils.sha256("admin123"),
                "管理员",
                "admin@bbs.local",
                "保密",
                "",
                true
        );
        users.put(key(admin.getUsername()), admin);
    }

    public synchronized boolean register(User user, String rawPassword) {
        String usernameKey = key(user.getUsername());
        if (users.containsKey(usernameKey)) {
            return false;
        }

        user.setPasswordHash(PasswordUtils.sha256(rawPassword));
        user.setAdmin(false);
        users.put(usernameKey, user);
        return true;
    }

    public synchronized User authenticate(String username, String rawPassword) {
        User user = users.get(key(username));
        if (user == null) {
            return null;
        }

        String passwordHash = PasswordUtils.sha256(rawPassword);
        return user.getPasswordHash().equals(passwordHash) ? user : null;
    }

    public synchronized Post addPost(String title, String content, String authorUsername) {
        Post post = new Post(nextPostId++, title, content, authorUsername, LocalDateTime.now());
        posts.add(0, post);
        return post;
    }

    public synchronized Post findPost(long id) {
        for (Post post : posts) {
            if (post.getId() == id) {
                return post;
            }
        }
        return null;
    }

    public synchronized boolean deletePost(long id) {
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getId() == id) {
                posts.remove(i);
                return true;
            }
        }
        return false;
    }

    public synchronized List<Post> findPosts(int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int fromIndex = (safePage - 1) * pageSize;
        if (fromIndex >= posts.size()) {
            return new ArrayList<>();
        }

        int toIndex = Math.min(fromIndex + pageSize, posts.size());
        return new ArrayList<>(posts.subList(fromIndex, toIndex));
    }

    public synchronized int countPosts() {
        return posts.size();
    }

    private String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
