package com.example.demo.servlet;

import com.example.demo.model.Post;
import com.example.demo.model.User;
import com.example.demo.repository.BbsRepository;
import com.example.demo.util.TextUtils;
import com.example.demo.util.WebUtil;

import java.io.IOException;
import java.sql.SQLException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/post/action")
public class PostActionServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = WebUtil.getLoginUser(request);
        if (user == null) {
            if (isAjax(request)) {
                writeJson(response, false, "请先登录");
            } else {
                WebUtil.setFlash(request, "请先登录");
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }

        long postId = WebUtil.parseLong(request.getParameter("id"), -1L);
        String action = TextUtils.trim(request.getParameter("action"));
        String message = "操作已完成";
        Post freshPost = null;
        try {
            BbsRepository repository = WebUtil.getRepository(getServletContext());
            if ("like".equals(action)) {
                repository.votePost(user, postId, 1);
                message = user.isOldUser() ? "已按老东西权限双倍点赞" : "已点赞";
            } else if ("dislike".equals(action)) {
                repository.votePost(user, postId, -1);
                message = user.isOldUser() ? "已按老东西权限双倍踩" : "已踩";
            } else if ("favorite".equals(action)) {
                repository.favoritePost(user, postId);
                message = "收藏状态已更新";
            } else if ("report".equals(action)) {
                repository.reportPost(user, postId, request.getParameter("reason"));
                message = user.isOldUser() ? "举报已提交，举报人数 +2" : "举报已提交";
            } else if ("delete".equals(action)) {
                repository.deletePost(postId, user);
                message = "帖子已删除";
                if (isAjax(request)) {
                    writeJson(response, true, message);
                } else {
                    WebUtil.setFlash(request, message);
                    response.sendRedirect(request.getContextPath() + "/posts");
                }
                return;
            } else {
                throw new IllegalArgumentException("未知操作");
            }
            freshPost = repository.findPost(postId);
        } catch (IllegalArgumentException | SQLException e) {
            if (isAjax(request)) {
                writeJson(response, false, e.getMessage());
            } else {
                WebUtil.setFlash(request, e.getMessage());
                redirectWithFragment(response, request.getContextPath() + "/post/detail?id=" + postId + "#actions");
            }
            return;
        }

        if (isAjax(request)) {
            writeJson(response, true, message, freshPost);
        } else {
            WebUtil.setFlash(request, message);
            redirectWithFragment(response, request.getContextPath() + "/post/detail?id=" + postId + "#actions");
        }
    }

    private void redirectWithFragment(HttpServletResponse response, String location) {
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", location);
    }

    private boolean isAjax(HttpServletRequest request) {
        return "fetch".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    private void writeJson(HttpServletResponse response, boolean ok, String message) throws IOException {
        writeJson(response, ok, message, null);
    }

    private void writeJson(HttpServletResponse response, boolean ok, String message, Post post) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        StringBuilder json = new StringBuilder();
        json.append("{\"ok\":").append(ok)
                .append(",\"message\":\"").append(jsonEscape(message)).append("\"");
        if (post != null) {
            json.append(",\"post\":{")
                    .append("\"id\":").append(post.getId())
                    .append(",\"likeScore\":").append(post.getLikeScore())
                    .append(",\"dislikeScore\":").append(post.getDislikeScore())
                    .append(",\"favoriteCount\":").append(post.getFavoriteCount())
                    .append(",\"commentCount\":").append(post.getCommentCount())
                    .append("}");
        }
        json.append("}");
        response.getWriter().write(json.toString());
    }

    private String jsonEscape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }
}
