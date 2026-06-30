package com.example.demo.repository;

import com.example.demo.model.Comment;
import com.example.demo.model.Notification;
import com.example.demo.model.Post;
import com.example.demo.model.PostSearchCriteria;
import com.example.demo.model.Report;
import com.example.demo.model.User;
import com.example.demo.util.DbUtil;
import com.example.demo.util.ForumOptions;
import com.example.demo.util.IpLocationUtil;
import com.example.demo.util.PasswordUtils;
import com.example.demo.util.TextUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcBbsRepository {
    public User register(String username, String password, String phone) throws SQLException {
        String normalizedUsername = TextUtils.trim(username);
        String normalizedPhone = normalizePhone(phone);
        validateUsername(normalizedUsername);
        validatePassword(password);
        validatePhone(normalizedPhone);

        try (Connection connection = DbUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (exists(connection, "SELECT 1 FROM users WHERE username = ?", normalizedUsername)) {
                    throw new IllegalArgumentException("用户名已存在");
                }
                if (exists(connection, "SELECT 1 FROM users WHERE phone = ?", normalizedPhone)) {
                    throw new IllegalArgumentException("电话已注册");
                }

                try (PreparedStatement prepared = connection.prepareStatement(
                        "INSERT INTO users(username, password_hash, phone, province, role, banned, banned_until, history_enabled, created_at, register_time) VALUES (?, ?, ?, ?, ?, 0, NULL, 1, NOW(), NOW())",
                        Statement.RETURN_GENERATED_KEYS)) {
                    prepared.setString(1, normalizedUsername);
                    prepared.setString(2, PasswordUtils.sha256(password));
                    prepared.setString(3, normalizedPhone);
                    prepared.setString(4, null);
                    prepared.setString(5, User.ROLE_NEW);
                    prepared.executeUpdate();
                    long id = readGeneratedId(prepared);
                    connection.commit();
                    return findUserById(id);
                }
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public User authenticateByPassword(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ?";
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement(sql)) {
            prepared.setString(1, TextUtils.trim(username));
            prepared.setString(2, PasswordUtils.sha256(TextUtils.trim(password)));
            try (ResultSet resultSet = prepared.executeQuery()) {
                return resultSet.next() ? mapUser(resultSet) : null;
            }
        }
    }

    public User authenticateByPhone(String phone) throws SQLException {
        String normalizedPhone = normalizePhone(phone);
        validatePhone(normalizedPhone);
        try (Connection connection = DbUtil.getConnection()) {
            return findUserByPhone(connection, normalizedPhone);
        }
    }

    public void resetPassword(String phone, String newPassword) throws SQLException {
        String normalizedPhone = normalizePhone(phone);
        validatePhone(normalizedPhone);
        validatePassword(newPassword);
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement("UPDATE users SET password_hash = ? WHERE phone = ?")) {
            prepared.setString(1, PasswordUtils.sha256(newPassword));
            prepared.setString(2, normalizedPhone);
            if (prepared.executeUpdate() == 0) {
                throw new IllegalArgumentException("该电话未注册");
            }
        }
    }

    public User findUserById(long id) throws SQLException {
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement("SELECT * FROM users WHERE id = ?")) {
            prepared.setLong(1, id);
            try (ResultSet resultSet = prepared.executeQuery()) {
                return resultSet.next() ? mapUser(resultSet) : null;
            }
        }
    }

    public List<User> findUsers() throws SQLException {
        return findUsers(1, 100);
    }

    public List<User> findUsers(int page, int pageSize) throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement("SELECT * FROM users ORDER BY role = 'ADMIN' DESC, id DESC LIMIT ? OFFSET ?")) {
            prepared.setInt(1, Math.max(1, pageSize));
            prepared.setInt(2, Math.max(0, (Math.max(page, 1) - 1) * Math.max(1, pageSize)));
            try (ResultSet resultSet = prepared.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
            }
        }
        return users;
    }

    public int countUsers() throws SQLException {
        return countRows("SELECT COUNT(*) FROM users");
    }

    public void setHistoryEnabled(long userId, boolean enabled) throws SQLException {
        executeUpdate("UPDATE users SET history_enabled = ? WHERE id = ?", enabled ? 1 : 0, userId);
        if (!enabled) {
            executeUpdate("DELETE FROM browse_history WHERE user_id = ?", userId);
        }
    }

    public User updateProfile(long userId, String username, String phone, String newPassword) throws SQLException {
        String normalizedUsername = TextUtils.trim(username);
        String normalizedPhone = normalizePhone(phone);
        String cleanPassword = TextUtils.trim(newPassword);
        validateUsername(normalizedUsername);
        validatePhone(normalizedPhone);
        if (!cleanPassword.isEmpty()) {
            validatePassword(cleanPassword);
        }

        try (Connection connection = DbUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (exists(connection, "SELECT 1 FROM users WHERE username = ? AND id <> ?", normalizedUsername, userId)) {
                    throw new IllegalArgumentException("用户名已存在");
                }
                if (exists(connection, "SELECT 1 FROM users WHERE phone = ? AND id <> ?", normalizedPhone, userId)) {
                    throw new IllegalArgumentException("电话已注册");
                }
                if (cleanPassword.isEmpty()) {
                    executeUpdate(connection, "UPDATE users SET username = ?, phone = ? WHERE id = ?",
                            normalizedUsername, normalizedPhone, userId);
                } else {
                    executeUpdate(connection, "UPDATE users SET username = ?, phone = ?, password_hash = ? WHERE id = ?",
                            normalizedUsername, normalizedPhone, PasswordUtils.sha256(cleanPassword), userId);
                }
                connection.commit();
                return findUserById(userId);
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void adminUpdateUser(long adminUserId, long userId, String phone, String role, boolean banned, int banDays) throws SQLException {
        if (adminUserId == userId && banned) {
            throw new IllegalArgumentException("管理员不能封禁自己的账号");
        }
        validatePhone(normalizePhone(phone));
        if (!User.ROLE_NEW.equals(role) && !User.ROLE_OLD.equals(role) && !User.ROLE_ADMIN.equals(role)) {
            throw new IllegalArgumentException("用户角色不正确");
        }
        try {
            if (banned) {
                if (banDays > 0) {
                    executeUpdate("UPDATE users SET phone = ?, role = ?, banned = 1, banned_until = DATE_ADD(NOW(), INTERVAL ? DAY) WHERE id = ?",
                            normalizePhone(phone), role, banDays, userId);
                } else {
                    executeUpdate("UPDATE users SET phone = ?, role = ?, banned = 1, banned_until = NULL WHERE id = ?",
                            normalizePhone(phone), role, userId);
                }
            } else {
                executeUpdate("UPDATE users SET phone = ?, role = ?, banned = 0, banned_until = NULL WHERE id = ?",
                        normalizePhone(phone), role, userId);
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                throw new IllegalArgumentException("电话已被其他用户使用");
            }
            throw e;
        }
    }

    public Post addPost(String title, String topic, String content, User author, String authorIp, String clientProvince) throws SQLException {
        requireActiveUser(author);
        String cleanTitle = TextUtils.trim(title);
        String cleanTopic = TextUtils.trim(topic);
        String cleanContent = TextUtils.trim(content);
        String cleanRegion = resolveProvince(clientProvince);
        String cleanAuthorIp = TextUtils.trim(authorIp);
        if (cleanAuthorIp.length() > 45) {
            cleanAuthorIp = cleanAuthorIp.substring(0, 45);
        }
        if (cleanTitle.isEmpty() || cleanTitle.length() > 120) {
            throw new IllegalArgumentException("帖子标题需为 1-120 个字符");
        }
        if (cleanTopic.isEmpty()) {
            throw new IllegalArgumentException("请选择主题");
        }
        if (cleanContent.isEmpty() || cleanContent.length() > 8000) {
            throw new IllegalArgumentException("正文需为 1-8000 个字符");
        }

        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement(
                     "INSERT INTO posts(title, topic, region, content, author_id, ip_address, pinned, deleted, like_score, dislike_score, favorite_count, comment_count, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0, 0, NOW(), NOW())",
                     Statement.RETURN_GENERATED_KEYS)) {
            prepared.setString(1, cleanTitle);
            prepared.setString(2, cleanTopic);
            prepared.setString(3, cleanRegion);
            prepared.setString(4, cleanContent);
            prepared.setLong(5, author.getId());
            prepared.setString(6, cleanAuthorIp.isEmpty() ? null : cleanAuthorIp);
            prepared.setInt(7, author.isAdmin() ? 1 : 0);
            prepared.executeUpdate();
            return findPost(readGeneratedId(prepared));
        }
    }

    public void updatePost(long postId, String title, String topic, String content, User user) throws SQLException {
        requireActiveUser(user);
        Post post = findPost(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        if (!user.isAdmin() && post.getAuthorId() != user.getId()) {
            throw new IllegalArgumentException("只能编辑自己发表的帖子");
        }
        String cleanTitle = TextUtils.trim(title);
        String cleanTopic = TextUtils.trim(topic);
        String cleanContent = TextUtils.trim(content);
        if (cleanTitle.isEmpty() || cleanTitle.length() > 120) {
            throw new IllegalArgumentException("帖子标题需为 1-120 个字符");
        }
        if (cleanTopic.isEmpty()) {
            throw new IllegalArgumentException("请选择主题");
        }
        if (cleanContent.isEmpty() || cleanContent.length() > 8000) {
            throw new IllegalArgumentException("正文需为 1-8000 个字符");
        }
        executeUpdate("UPDATE posts SET title = ?, topic = ?, content = ? WHERE id = ?",
                cleanTitle, cleanTopic, cleanContent, postId);
    }

    public void deletePost(long postId, User user) throws SQLException {
        Post post = findPost(postId);
        if (post == null) {
            return;
        }
        if (!user.isAdmin() && post.getAuthorId() != user.getId()) {
            throw new IllegalArgumentException("只能删除自己的帖子");
        }
        executeUpdate("UPDATE posts SET deleted = 1 WHERE id = ?", postId);
    }

    public void setPostPinned(long postId, boolean pinned) throws SQLException {
        executeUpdate("UPDATE posts SET pinned = ? WHERE id = ?", pinned ? 1 : 0, postId);
    }

    public Post findPost(long id) throws SQLException {
        String sql = basePostSql() + " WHERE p.id = ? AND p.deleted = 0";
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement(sql)) {
            prepared.setLong(1, id);
            try (ResultSet resultSet = prepared.executeQuery()) {
                return resultSet.next() ? mapPost(resultSet) : null;
            }
        }
    }

    public List<Post> searchPosts(PostSearchCriteria criteria, int page, int pageSize) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder(basePostSql()).append(" WHERE p.deleted = 0");
        appendPostCriteria(sql, params, criteria);
        appendPostOrder(sql, criteria);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(Math.max(0, (Math.max(page, 1) - 1) * pageSize));

        List<Post> posts = new ArrayList<>();
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement(sql.toString())) {
            fillParams(prepared, params);
            try (ResultSet resultSet = prepared.executeQuery()) {
                while (resultSet.next()) {
                    posts.add(mapPost(resultSet));
                }
            }
        }
        return posts;
    }

    public List<String> findTopics() throws SQLException {
        List<String> topics = new ArrayList<>();
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement("SELECT DISTINCT topic FROM posts WHERE deleted = 0 ORDER BY topic ASC");
             ResultSet resultSet = prepared.executeQuery()) {
            while (resultSet.next()) {
                topics.add(resultSet.getString("topic"));
            }
        }
        return topics;
    }

    public int countPosts(PostSearchCriteria criteria) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM posts p JOIN users u ON p.author_id = u.id WHERE p.deleted = 0");
        appendPostCriteria(sql, params, criteria);
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement(sql.toString())) {
            fillParams(prepared, params);
            try (ResultSet resultSet = prepared.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public void addHistory(User user, long postId) throws SQLException {
        if (user == null || !user.isHistoryEnabled()) {
            return;
        }
        executeUpdate("INSERT INTO browse_history(user_id, post_id, viewed_at) VALUES (?, ?, NOW()) "
                + "ON DUPLICATE KEY UPDATE viewed_at = NOW()", user.getId(), postId);
    }

    public List<Post> findHistory(long userId) throws SQLException {
        return findHistory(userId, 1, 50);
    }

    public List<Post> findHistory(long userId, int page, int pageSize) throws SQLException {
        return findPostList("SELECT p.*, u.username AS author_username, "
                + "CASE WHEN u.role = 'ADMIN' THEN 'ADMIN' WHEN u.register_time <= DATE_SUB(NOW(), INTERVAL 6 MONTH) THEN 'OLD_USER' ELSE 'NEW_USER' END AS author_role "
                + "FROM browse_history h JOIN posts p ON h.post_id = p.id JOIN users u ON p.author_id = u.id "
                + "WHERE h.user_id = ? AND p.deleted = 0 ORDER BY h.viewed_at DESC LIMIT ? OFFSET ?",
                userId, Math.max(1, pageSize), Math.max(0, (Math.max(page, 1) - 1) * Math.max(1, pageSize)));
    }

    public int countHistory(long userId) throws SQLException {
        return countRows("SELECT COUNT(*) FROM browse_history h JOIN posts p ON h.post_id = p.id WHERE h.user_id = ? AND p.deleted = 0", userId);
    }

    public List<Post> findFavorites(long userId) throws SQLException {
        return findFavorites(userId, 1, 50);
    }

    public List<Post> findFavorites(long userId, int page, int pageSize) throws SQLException {
        return findPostList("SELECT p.*, u.username AS author_username, "
                + "CASE WHEN u.role = 'ADMIN' THEN 'ADMIN' WHEN u.register_time <= DATE_SUB(NOW(), INTERVAL 6 MONTH) THEN 'OLD_USER' ELSE 'NEW_USER' END AS author_role "
                + "FROM favorites f JOIN posts p ON f.post_id = p.id JOIN users u ON p.author_id = u.id "
                + "WHERE f.user_id = ? AND p.deleted = 0 ORDER BY f.created_at DESC LIMIT ? OFFSET ?",
                userId, Math.max(1, pageSize), Math.max(0, (Math.max(page, 1) - 1) * Math.max(1, pageSize)));
    }

    public int countFavorites(long userId) throws SQLException {
        return countRows("SELECT COUNT(*) FROM favorites f JOIN posts p ON f.post_id = p.id WHERE f.user_id = ? AND p.deleted = 0", userId);
    }

    public List<Post> findMyPosts(long userId) throws SQLException {
        return findMyPosts(userId, 1, 50);
    }

    public List<Post> findMyPosts(long userId, int page, int pageSize) throws SQLException {
        return findPostList(basePostSql() + " WHERE p.author_id = ? AND p.deleted = 0 ORDER BY p.created_at DESC LIMIT ? OFFSET ?",
                userId, Math.max(1, pageSize), Math.max(0, (Math.max(page, 1) - 1) * Math.max(1, pageSize)));
    }

    public int countMyPosts(long userId) throws SQLException {
        return countRows("SELECT COUNT(*) FROM posts p WHERE p.author_id = ? AND p.deleted = 0", userId);
    }

    public void votePost(User user, long postId, int value) throws SQLException {
        requireActiveUser(user);
        if (value != 1 && value != -1) {
            throw new IllegalArgumentException("投票类型不正确");
        }
        Post post = findPost(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        int weight = user.isOldUser() ? 2 : 1;
        changeVote("post_votes", "post_id", "posts", postId, user.getId(), value, weight);
        if (value == 1) {
            notifyPostOwner(post, user, "LIKE", user.getUsername() + " 点赞了你的帖子");
        }
    }

    public void favoritePost(User user, long postId) throws SQLException {
        requireActiveUser(user);
        Post post = findPost(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        try (Connection connection = DbUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (exists(connection, "SELECT 1 FROM favorites WHERE user_id = ? AND post_id = ?", user.getId(), postId)) {
                    executeUpdate(connection, "DELETE FROM favorites WHERE user_id = ? AND post_id = ?", user.getId(), postId);
                    executeUpdate(connection, "UPDATE posts SET favorite_count = GREATEST(favorite_count - 1, 0) WHERE id = ?", postId);
                } else {
                    executeUpdate(connection, "INSERT INTO favorites(user_id, post_id, created_at) VALUES (?, ?, NOW())", user.getId(), postId);
                    executeUpdate(connection, "UPDATE posts SET favorite_count = favorite_count + 1 WHERE id = ?", postId);
                    notifyPostOwner(connection, post, user, "FAVORITE", user.getUsername() + " 收藏了你的帖子");
                }
                connection.commit();
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void reportPost(User user, long postId, String reason) throws SQLException {
        requireActiveUser(user);
        if (user.isAdmin()) {
            throw new IllegalArgumentException("管理员不能举报");
        }
        if (TextUtils.trim(reason).isEmpty()) {
            throw new IllegalArgumentException("请输入举报原因");
        }
        Post post = findPost(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        try (Connection connection = DbUtil.getConnection()) {
            if (exists(connection, "SELECT 1 FROM reports WHERE reporter_id = ? AND post_id = ?", user.getId(), postId)) {
                throw new IllegalArgumentException("同一个帖子只能举报一次");
            }
        }
        int weight = user.isOldUser() ? 2 : 1;
        executeUpdate("INSERT INTO reports(reporter_id, post_id, reason, weight, handled, created_at) VALUES (?, ?, ?, ?, 0, NOW())",
                user.getId(), postId, TextUtils.trim(reason), weight);
    }

    public Comment addComment(User user, long postId, long parentCommentId, String content,
                              String authorIp, String clientProvince) throws SQLException {
        requireActiveUser(user);
        Post post = findPost(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        Comment parentComment = null;
        if (parentCommentId > 0) {
            parentComment = findComment(parentCommentId);
            if (parentComment == null || parentComment.getPostId() != postId) {
                throw new IllegalArgumentException("回复的评论不存在");
            }
        }
        String cleanContent = TextUtils.trim(content);
        String cleanRegion = resolveProvince(clientProvince);
        String cleanAuthorIp = TextUtils.trim(authorIp);
        if (cleanAuthorIp.length() > 45) {
            cleanAuthorIp = cleanAuthorIp.substring(0, 45);
        }
        if (cleanContent.isEmpty() || cleanContent.length() > 2000) {
            throw new IllegalArgumentException("评论需为 1-2000 个字符");
        }

        try (Connection connection = DbUtil.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement prepared = connection.prepareStatement(
                    "INSERT INTO comments(post_id, parent_comment_id, author_id, ip_address, region, content, deleted, like_score, dislike_score, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 0, 0, 0, NOW(), NOW())",
                    Statement.RETURN_GENERATED_KEYS)) {
                prepared.setLong(1, postId);
                if (parentCommentId > 0) {
                    prepared.setLong(2, parentCommentId);
                } else {
                    prepared.setObject(2, null);
                }
                prepared.setLong(3, user.getId());
                prepared.setString(4, cleanAuthorIp.isEmpty() ? null : cleanAuthorIp);
                prepared.setString(5, cleanRegion);
                prepared.setString(6, cleanContent);
                prepared.executeUpdate();
                long commentId = readGeneratedId(prepared);
                executeUpdate(connection, "UPDATE posts SET comment_count = comment_count + 1 WHERE id = ?", postId);
                if (parentComment == null) {
                    notifyPostOwner(connection, post, user, "COMMENT", user.getUsername() + " 评论了你的帖子", commentId);
                } else if (parentComment.getAuthorId() != user.getId()) {
                    notifyCommentOwner(connection, parentComment, user, postId, commentId);
                }
                connection.commit();
                return findComment(commentId);
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public List<Comment> findComments(long postId) throws SQLException {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT c.*, u.username AS author_username, parent_user.username AS parent_author_username "
                + "FROM comments c JOIN users u ON c.author_id = u.id "
                + "LEFT JOIN comments parent_comment ON c.parent_comment_id = parent_comment.id "
                + "LEFT JOIN users parent_user ON parent_comment.author_id = parent_user.id "
                + "WHERE c.post_id = ? AND c.deleted = 0 ORDER BY c.created_at ASC";
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement(sql)) {
            prepared.setLong(1, postId);
            try (ResultSet resultSet = prepared.executeQuery()) {
                while (resultSet.next()) {
                    comments.add(mapComment(resultSet));
                }
            }
        }
        return comments;
    }

    public Comment findComment(long commentId) throws SQLException {
        String sql = "SELECT c.*, u.username AS author_username, parent_user.username AS parent_author_username "
                + "FROM comments c JOIN users u ON c.author_id = u.id "
                + "LEFT JOIN comments parent_comment ON c.parent_comment_id = parent_comment.id "
                + "LEFT JOIN users parent_user ON parent_comment.author_id = parent_user.id "
                + "WHERE c.id = ? AND c.deleted = 0";
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement(sql)) {
            prepared.setLong(1, commentId);
            try (ResultSet resultSet = prepared.executeQuery()) {
                return resultSet.next() ? mapComment(resultSet) : null;
            }
        }
    }

    public void updateComment(User user, long commentId, String content) throws SQLException {
        requireActiveUser(user);
        Comment comment = findComment(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        if (!user.isAdmin() && comment.getAuthorId() != user.getId()) {
            throw new IllegalArgumentException("只能编辑自己的评论");
        }
        executeUpdate("UPDATE comments SET content = ? WHERE id = ?", TextUtils.trim(content), commentId);
    }

    public void deleteComment(User user, long commentId) throws SQLException {
        Comment comment = findComment(commentId);
        if (comment == null) {
            return;
        }
        if (!user.isAdmin() && comment.getAuthorId() != user.getId()) {
            throw new IllegalArgumentException("只能删除自己的评论");
        }
        executeUpdate("UPDATE comments SET deleted = 1 WHERE id = ?", commentId);
        executeUpdate("UPDATE posts SET comment_count = GREATEST(comment_count - 1, 0) WHERE id = ?", comment.getPostId());
    }

    public void voteComment(User user, long commentId, int value) throws SQLException {
        requireActiveUser(user);
        if (value != 1 && value != -1) {
            throw new IllegalArgumentException("投票类型不正确");
        }
        Comment comment = findComment(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        int weight = user.isOldUser() ? 2 : 1;
        changeVote("comment_votes", "comment_id", "comments", commentId, user.getId(), value, weight);
        if (value == 1 && comment.getAuthorId() != user.getId()) {
            executeUpdate("INSERT INTO notifications(recipient_id, actor_id, post_id, comment_id, type, message, read_flag, created_at) VALUES (?, ?, ?, ?, ?, ?, 0, NOW())",
                    comment.getAuthorId(), user.getId(), comment.getPostId(), commentId, "LIKE", user.getUsername() + " 点赞了你的评论");
        }
    }

    public void reportComment(User user, long commentId, String reason) throws SQLException {
        requireActiveUser(user);
        if (user.isAdmin()) {
            throw new IllegalArgumentException("管理员不能举报");
        }
        if (TextUtils.trim(reason).isEmpty()) {
            throw new IllegalArgumentException("请输入举报原因");
        }
        Comment comment = findComment(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        try (Connection connection = DbUtil.getConnection()) {
            if (exists(connection, "SELECT 1 FROM reports WHERE reporter_id = ? AND comment_id = ?", user.getId(), commentId)) {
                throw new IllegalArgumentException("同一条评论只能举报一次");
            }
        }
        int weight = user.isOldUser() ? 2 : 1;
        executeUpdate("INSERT INTO reports(reporter_id, comment_id, reason, weight, handled, created_at) VALUES (?, ?, ?, ?, 0, NOW())",
                user.getId(), commentId, TextUtils.trim(reason), weight);
    }

    public List<Notification> findNotifications(long userId) throws SQLException {
        return findNotifications(userId, 1, 100);
    }

    public List<Notification> findNotifications(long userId, int page, int pageSize) throws SQLException {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT n.*, a.username AS actor_username, p.title AS post_title "
                + "FROM notifications n JOIN users a ON n.actor_id = a.id LEFT JOIN posts p ON n.post_id = p.id "
                + "WHERE n.recipient_id = ? ORDER BY n.created_at DESC LIMIT ? OFFSET ?";
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement(sql)) {
            prepared.setLong(1, userId);
            prepared.setInt(2, Math.max(1, pageSize));
            prepared.setInt(3, Math.max(0, (Math.max(page, 1) - 1) * Math.max(1, pageSize)));
            try (ResultSet resultSet = prepared.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(new Notification(
                            resultSet.getLong("id"),
                            resultSet.getLong("post_id"),
                            resultSet.getLong("comment_id"),
                            resultSet.getString("type"),
                            resultSet.getString("actor_username"),
                            resultSet.getString("post_title"),
                            resultSet.getString("message"),
                            resultSet.getBoolean("read_flag"),
                            toLocalDateTime(resultSet.getTimestamp("created_at"))
                    ));
                }
            }
        }
        executeUpdate("UPDATE notifications SET read_flag = 1 WHERE recipient_id = ?", userId);
        return notifications;
    }

    public int countNotifications(long userId) throws SQLException {
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement("SELECT COUNT(*) FROM notifications WHERE recipient_id = ?")) {
            prepared.setLong(1, userId);
            try (ResultSet resultSet = prepared.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public int countUnreadNotifications(long userId) throws SQLException {
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement("SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND read_flag = 0")) {
            prepared.setLong(1, userId);
            try (ResultSet resultSet = prepared.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    public List<Report> findReports() throws SQLException {
        return findReports(1, 100);
    }

    public List<Report> findReports(int page, int pageSize) throws SQLException {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM (" + groupedReportsSql() + ") grouped_reports "
                + "ORDER BY handled ASC, created_at DESC LIMIT ? OFFSET ?";
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement(sql)) {
            prepared.setInt(1, Math.max(1, pageSize));
            prepared.setInt(2, Math.max(0, (Math.max(page, 1) - 1) * Math.max(1, pageSize)));
            try (ResultSet resultSet = prepared.executeQuery()) {
                while (resultSet.next()) {
                    reports.add(new Report(
                            resultSet.getLong("id"),
                            resultSet.getLong("post_id"),
                            resultSet.getLong("comment_id"),
                            resultSet.getLong("target_user_id"),
                            resultSet.getString("post_title"),
                            resultSet.getString("target_type"),
                            resultSet.getString("target_username"),
                            resultSet.getString("reporter_username"),
                            resultSet.getString("reason"),
                            resultSet.getInt("report_count"),
                            resultSet.getInt("report_count"),
                            resultSet.getBoolean("handled"),
                            toLocalDateTime(resultSet.getTimestamp("created_at"))
                    ));
                }
            }
        }
        return reports;
    }

    public int countReports() throws SQLException {
        String sql = "SELECT COUNT(*) FROM (" + groupedReportsSql() + ") grouped_reports";
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement(sql);
             ResultSet resultSet = prepared.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private String groupedReportsSql() {
        return "SELECT MIN(r.id) AS id, r.post_id AS post_id, 0 AS comment_id, p.author_id AS target_user_id, "
                + "p.title AS post_title, 'post' AS target_type, target_user.username AS target_username, "
                + "GROUP_CONCAT(DISTINCT reporter.username ORDER BY reporter.username SEPARATOR '、') AS reporter_username, "
                + "GROUP_CONCAT(DISTINCT r.reason ORDER BY r.created_at SEPARATOR '；') AS reason, "
                + "SUM(r.weight) AS report_count, MIN(r.handled) AS handled, MAX(r.created_at) AS created_at "
                + "FROM reports r "
                + "JOIN posts p ON r.post_id = p.id "
                + "JOIN users target_user ON p.author_id = target_user.id "
                + "JOIN users reporter ON r.reporter_id = reporter.id "
                + "WHERE r.post_id IS NOT NULL "
                + "GROUP BY r.post_id, p.author_id, p.title, target_user.username "
                + "UNION ALL "
                + "SELECT MIN(r.id) AS id, c.post_id AS post_id, r.comment_id AS comment_id, c.author_id AS target_user_id, "
                + "p.title AS post_title, 'comment' AS target_type, target_user.username AS target_username, "
                + "GROUP_CONCAT(DISTINCT reporter.username ORDER BY reporter.username SEPARATOR '、') AS reporter_username, "
                + "GROUP_CONCAT(DISTINCT r.reason ORDER BY r.created_at SEPARATOR '；') AS reason, "
                + "SUM(r.weight) AS report_count, MIN(r.handled) AS handled, MAX(r.created_at) AS created_at "
                + "FROM reports r "
                + "JOIN comments c ON r.comment_id = c.id "
                + "JOIN posts p ON c.post_id = p.id "
                + "JOIN users target_user ON c.author_id = target_user.id "
                + "JOIN users reporter ON r.reporter_id = reporter.id "
                + "WHERE r.comment_id IS NOT NULL "
                + "GROUP BY r.comment_id, c.post_id, c.author_id, p.title, target_user.username";
    }

    public void markReportHandled(long reportId) throws SQLException {
        executeUpdate("UPDATE reports SET handled = 1 WHERE id = ?", reportId);
    }

    public void handleReport(String targetType, long targetId, long adminUserId, int banDays) throws SQLException {
        if ("comment".equals(targetType)) {
            Comment comment = findComment(targetId);
            if (comment == null) {
                executeUpdate("UPDATE reports SET handled = 1 WHERE comment_id = ?", targetId);
                return;
            }
            if (banDays > 0) {
                banUserFromReport(adminUserId, comment.getAuthorId(), banDays);
            }
            executeUpdate("UPDATE reports SET handled = 1 WHERE comment_id = ?", targetId);
            return;
        }

        Post post = findPost(targetId);
        if (post != null && banDays > 0) {
            banUserFromReport(adminUserId, post.getAuthorId(), banDays);
        }
        executeUpdate("UPDATE reports SET handled = 1 WHERE post_id = ?", targetId);
    }

    private void banUserFromReport(long adminUserId, long targetUserId, int banDays) throws SQLException {
        if (adminUserId == targetUserId) {
            throw new IllegalArgumentException("管理员不能封禁自己的账号");
        }
        User target = findUserById(targetUserId);
        if (target != null && target.isAdmin()) {
            throw new IllegalArgumentException("不能通过举报处理封禁管理员");
        }
        executeUpdate("UPDATE users SET banned = 1, banned_until = DATE_ADD(NOW(), INTERVAL ? DAY) WHERE id = ?", banDays, targetUserId);
    }

    private void changeVote(String voteTable, String targetColumn, String targetTable, long targetId, long userId, int value, int weight) throws SQLException {
        try (Connection connection = DbUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Integer oldValue = null;
                Integer oldWeight = null;
                try (PreparedStatement prepared = connection.prepareStatement(
                        "SELECT value, weight FROM " + voteTable + " WHERE user_id = ? AND " + targetColumn + " = ?")) {
                    prepared.setLong(1, userId);
                    prepared.setLong(2, targetId);
                    try (ResultSet resultSet = prepared.executeQuery()) {
                        if (resultSet.next()) {
                            oldValue = resultSet.getInt("value");
                            oldWeight = resultSet.getInt("weight");
                        }
                    }
                }

                if (oldValue == null) {
                    executeUpdate(connection, "INSERT INTO " + voteTable + "(user_id, " + targetColumn + ", value, weight, created_at) VALUES (?, ?, ?, ?, NOW())",
                            userId, targetId, value, weight);
                    adjustScore(connection, targetTable, targetId, value, weight);
                } else if (oldValue == value) {
                    executeUpdate(connection, "DELETE FROM " + voteTable + " WHERE user_id = ? AND " + targetColumn + " = ?", userId, targetId);
                    adjustScore(connection, targetTable, targetId, oldValue, -oldWeight);
                } else {
                    executeUpdate(connection, "UPDATE " + voteTable + " SET value = ?, weight = ? WHERE user_id = ? AND " + targetColumn + " = ?",
                            value, weight, userId, targetId);
                    adjustScore(connection, targetTable, targetId, oldValue, -oldWeight);
                    adjustScore(connection, targetTable, targetId, value, weight);
                }
                connection.commit();
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void adjustScore(Connection connection, String targetTable, long targetId, int value, int delta) throws SQLException {
        String column = value == 1 ? "like_score" : "dislike_score";
        executeUpdate(connection, "UPDATE " + targetTable + " SET " + column + " = GREATEST(" + column + " + ?, 0) WHERE id = ?",
                delta, targetId);
    }

    private void notifyPostOwner(Post post, User actor, String type, String message) throws SQLException {
        try (Connection connection = DbUtil.getConnection()) {
            notifyPostOwner(connection, post, actor, type, message);
        }
    }

    private void notifyPostOwner(Connection connection, Post post, User actor, String type, String message) throws SQLException {
        notifyPostOwner(connection, post, actor, type, message, 0L);
    }

    private void notifyPostOwner(Connection connection, Post post, User actor, String type, String message, long commentId) throws SQLException {
        if (post.getAuthorId() == actor.getId()) {
            return;
        }
        executeUpdate(connection, "INSERT INTO notifications(recipient_id, actor_id, post_id, comment_id, type, message, read_flag, created_at) VALUES (?, ?, ?, ?, ?, ?, 0, NOW())",
                post.getAuthorId(), actor.getId(), post.getId(), commentId > 0 ? commentId : null, type, message);
    }

    private void notifyCommentOwner(Connection connection, Comment parentComment, User actor, long postId, long commentId) throws SQLException {
        executeUpdate(connection, "INSERT INTO notifications(recipient_id, actor_id, post_id, comment_id, type, message, read_flag, created_at) VALUES (?, ?, ?, ?, ?, ?, 0, NOW())",
                parentComment.getAuthorId(), actor.getId(), postId, commentId, "REPLY", actor.getUsername() + " 回复了你的评论");
    }

    private List<Post> findPostList(String sql, Object... params) throws SQLException {
        List<Post> posts = new ArrayList<>();
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement(sql)) {
            fillParams(prepared, asList(params));
            try (ResultSet resultSet = prepared.executeQuery()) {
                while (resultSet.next()) {
                    posts.add(mapPost(resultSet));
                }
            }
        }
        return posts;
    }

    private int countRows(String sql, Object... params) throws SQLException {
        try (Connection connection = DbUtil.getConnection();
             PreparedStatement prepared = connection.prepareStatement(sql)) {
            fillParams(prepared, asList(params));
            try (ResultSet resultSet = prepared.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private String basePostSql() {
        return "SELECT p.*, u.username AS author_username, "
                + "CASE WHEN u.role = 'ADMIN' THEN 'ADMIN' WHEN u.register_time <= DATE_SUB(NOW(), INTERVAL 6 MONTH) THEN 'OLD_USER' ELSE 'NEW_USER' END AS author_role "
                + "FROM posts p JOIN users u ON p.author_id = u.id";
    }

    private void appendPostCriteria(StringBuilder sql, List<Object> params, PostSearchCriteria criteria) {
        if (criteria == null) {
            return;
        }
        if (!isBlank(criteria.getTopic())) {
            sql.append(" AND p.topic = ?");
            params.add(criteria.getTopic());
        }
        if (!isBlank(criteria.getKeyword())) {
            sql.append(" AND p.content LIKE ?");
            params.add("%" + criteria.getKeyword() + "%");
        }
        if (criteria.getDays() > 0) {
            int days = Math.min(criteria.getDays(), 3650);
            sql.append(" AND p.created_at >= DATE_SUB(NOW(), INTERVAL ").append(days).append(" DAY)");
        }
        if (criteria.getMinLikes() > 0) {
            sql.append(" AND p.like_score >= ?");
            params.add(criteria.getMinLikes());
        }
        if (criteria.getMinFavorites() > 0) {
            sql.append(" AND p.favorite_count >= ?");
            params.add(criteria.getMinFavorites());
        }
    }

    private void appendPostOrder(StringBuilder sql, PostSearchCriteria criteria) {
        String orderBy = criteria == null ? "" : TextUtils.trim(criteria.getOrderBy());
        sql.append(" ORDER BY p.pinned DESC, u.role = 'ADMIN' DESC, u.register_time <= DATE_SUB(NOW(), INTERVAL 6 MONTH) DESC, ");
        if ("time".equals(orderBy)) {
            sql.append("p.created_at DESC");
        } else if ("favorites".equals(orderBy)) {
            sql.append("p.favorite_count DESC, p.like_score DESC, p.created_at DESC");
        } else {
            sql.append("p.like_score DESC, p.favorite_count DESC, p.created_at DESC");
        }
    }

    private void fillParams(PreparedStatement prepared, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            prepared.setObject(i + 1, params.get(i));
        }
    }

    private boolean exists(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement prepared = connection.prepareStatement(sql)) {
            fillParams(prepared, asList(params));
            try (ResultSet resultSet = prepared.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private User findUserByPhone(Connection connection, String phone) throws SQLException {
        try (PreparedStatement prepared = connection.prepareStatement("SELECT * FROM users WHERE phone = ?")) {
            prepared.setString(1, phone);
            try (ResultSet resultSet = prepared.executeQuery()) {
                return resultSet.next() ? mapUser(resultSet) : null;
            }
        }
    }

    private long readGeneratedId(PreparedStatement prepared) throws SQLException {
        try (ResultSet keys = prepared.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("未能获取新增记录编号");
            }
            return keys.getLong(1);
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                resultSet.getString("phone"),
                resultSet.getString("province"),
                resultSet.getString("role"),
                resultSet.getBoolean("banned"),
                toLocalDateTime(resultSet.getTimestamp("banned_until")),
                resultSet.getBoolean("history_enabled"),
                toLocalDateTime(resultSet.getTimestamp("created_at")),
                toLocalDateTime(resultSet.getTimestamp("register_time"))
        );
    }

    private Post mapPost(ResultSet resultSet) throws SQLException {
        return new Post(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("topic"),
                mapRegion(resultSet),
                resultSet.getString("content"),
                resultSet.getLong("author_id"),
                resultSet.getString("author_username"),
                resultSet.getString("author_role"),
                resultSet.getString("ip_address"),
                resultSet.getBoolean("pinned"),
                resultSet.getBoolean("deleted"),
                resultSet.getInt("like_score"),
                resultSet.getInt("dislike_score"),
                resultSet.getInt("favorite_count"),
                resultSet.getInt("comment_count"),
                toLocalDateTime(resultSet.getTimestamp("created_at")),
                toLocalDateTime(resultSet.getTimestamp("updated_at"))
        );
    }

    private String mapRegion(ResultSet resultSet) throws SQLException {
        return resolveStoredRegion(resultSet.getString("region"), resultSet.getString("ip_address"));
    }

    private String resolveStoredRegion(String region, String ipAddress) {
        String cleanRegion = TextUtils.trim(region);
        if (IpLocationUtil.LOCAL_REGION.equals(cleanRegion)) {
            return cleanRegion;
        }

        String province = ForumOptions.normalizeProvince(region);
        if (!province.isEmpty()) {
            return province;
        }

        province = IpLocationUtil.resolveProvince(ipAddress);
        if (!province.isEmpty() && !IpLocationUtil.UNKNOWN_REGION.equals(province)) {
            return province;
        }
        return IpLocationUtil.UNKNOWN_REGION;
    }

    private Comment mapComment(ResultSet resultSet) throws SQLException {
        return new Comment(
                resultSet.getLong("id"),
                resultSet.getLong("post_id"),
                resultSet.getLong("parent_comment_id"),
                resultSet.getLong("author_id"),
                resultSet.getString("author_username"),
                resultSet.getString("parent_author_username"),
                resultSet.getString("ip_address"),
                resolveStoredRegion(resultSet.getString("region"), resultSet.getString("ip_address")),
                resultSet.getString("content"),
                resultSet.getInt("like_score"),
                resultSet.getInt("dislike_score"),
                resultSet.getBoolean("deleted"),
                toLocalDateTime(resultSet.getTimestamp("created_at")),
                toLocalDateTime(resultSet.getTimestamp("updated_at"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private void executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection connection = DbUtil.getConnection()) {
            executeUpdate(connection, sql, params);
        }
    }

    private void executeUpdate(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement prepared = connection.prepareStatement(sql)) {
            fillParams(prepared, asList(params));
            prepared.executeUpdate();
        }
    }

    private List<Object> asList(Object... params) {
        List<Object> list = new ArrayList<>();
        if (params != null) {
            for (Object param : params) {
                list.add(param);
            }
        }
        return list;
    }

    private void requireActiveUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (user.isBanned()) {
            throw new IllegalArgumentException("账号已被封禁，不能执行该操作");
        }
    }

    private void validateUsername(String username) {
        if (username == null || !username.matches("[A-Za-z0-9_]{3,20}")) {
            throw new IllegalArgumentException("用户名需为 3-20 位字母、数字或下划线");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.trim().length() < 6 || password.trim().length() > 32) {
            throw new IllegalArgumentException("密码长度需为 6-32 位");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("[0-9]{5,20}")) {
            throw new IllegalArgumentException("电话需为 5-20 位数字");
        }
    }

    private String normalizePhone(String phone) {
        return TextUtils.trim(phone).replaceAll("\\s+", "");
    }

    private String resolveProvince(String province) {
        String cleanProvince = TextUtils.trim(province);
        String normalized = ForumOptions.normalizeProvince(cleanProvince);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        if (IpLocationUtil.LOCAL_REGION.equals(cleanProvince) || IpLocationUtil.UNKNOWN_REGION.equals(cleanProvince)) {
            return cleanProvince;
        }
        return IpLocationUtil.UNKNOWN_REGION;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
