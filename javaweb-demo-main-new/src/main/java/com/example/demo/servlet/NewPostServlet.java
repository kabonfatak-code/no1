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

@WebServlet("/post/new")
public class NewPostServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureLogin(request, response)) {
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/new-post.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User loginUser = WebUtil.getLoginUser(request);
        if (loginUser == null) {
            WebUtil.setFlash(request, "请先登录");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String topic = TextUtils.trim(request.getParameter("topic"));
        String content = TextUtils.trim(request.getParameter("content"));
        String title = deriveTitle(content);
        request.setAttribute("topic", topic);
        request.setAttribute("content", content);

        try {
            BbsRepository repository = WebUtil.getRepository(getServletContext());
            Post post = repository.addPost(
                    title,
                    topic,
                    content,
                    loginUser,
                    WebUtil.getClientIp(request),
                    WebUtil.getClientProvince(request)
            );
            WebUtil.setFlash(request, "帖子发表成功");
            response.sendRedirect(request.getContextPath() + "/post/detail?id=" + post.getId());
        } catch (IllegalArgumentException | SQLException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/new-post.jsp").forward(request, response);
        }
    }

    private boolean ensureLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (WebUtil.getLoginUser(request) != null) {
            return true;
        }

        WebUtil.setFlash(request, "请先登录");
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }

    private String deriveTitle(String content) {
        String compact = TextUtils.trim(content).replaceAll("\\s+", " ");
        if (compact.isEmpty()) {
            return "未命名帖子";
        }
        return compact.length() > 60 ? compact.substring(0, 60) + "..." : compact;
    }
}
