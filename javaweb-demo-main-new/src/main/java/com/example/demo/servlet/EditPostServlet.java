package com.example.demo.servlet;

import com.example.demo.model.Post;
import com.example.demo.model.User;
import com.example.demo.repository.BbsRepository;
import com.example.demo.util.TextUtils;
import com.example.demo.util.WebUtil;

import java.io.IOException;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/post/edit")
public class EditPostServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = WebUtil.getLoginUser(request);
        if (user == null) {
            WebUtil.setFlash(request, "请先登录");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try {
            Post post = WebUtil.getRepository(getServletContext()).findPost(WebUtil.parseLong(request.getParameter("id"), -1L));
            if (post == null || (!user.isAdmin() && post.getAuthorId() != user.getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            request.setAttribute("post", post);
            request.getRequestDispatcher("/WEB-INF/views/edit-post.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = WebUtil.getLoginUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        long postId = WebUtil.parseLong(request.getParameter("id"), -1L);
        String content = TextUtils.trim(request.getParameter("content"));
        try {
            WebUtil.getRepository(getServletContext()).updatePost(
                    postId,
                    deriveTitle(content),
                    TextUtils.trim(request.getParameter("topic")),
                    content,
                    user
            );
            WebUtil.setFlash(request, "帖子已更新");
            response.sendRedirect(request.getContextPath() + "/post/detail?id=" + postId);
        } catch (IllegalArgumentException | SQLException e) {
            request.setAttribute("error", e.getMessage());
            doGet(request, response);
        }
    }

    private String deriveTitle(String content) {
        String compact = TextUtils.trim(content).replaceAll("\\s+", " ");
        if (compact.isEmpty()) {
            return "未命名帖子";
        }
        return compact.length() > 60 ? compact.substring(0, 60) + "..." : compact;
    }
}
