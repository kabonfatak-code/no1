package com.example.demo.servlet;

import com.example.demo.model.Post;
import com.example.demo.model.User;
import com.example.demo.repository.BbsRepository;
import com.example.demo.util.TextUtils;
import com.example.demo.util.WebUtil;

import java.io.IOException;

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
        request.setCharacterEncoding("UTF-8");
        User loginUser = WebUtil.getLoginUser(request);
        if (loginUser == null) {
            WebUtil.setFlash(request, "请先登录");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String title = TextUtils.trim(request.getParameter("title"));
        String content = TextUtils.trim(request.getParameter("content"));
        request.setAttribute("title", title);
        request.setAttribute("content", content);

        String error = validate(title, content);
        if (error != null) {
            request.setAttribute("error", error);
            request.getRequestDispatcher("/WEB-INF/views/new-post.jsp").forward(request, response);
            return;
        }

        BbsRepository repository = WebUtil.getRepository(getServletContext());
        Post post = repository.addPost(title, content, loginUser.getUsername());
        WebUtil.setFlash(request, "留言发表成功");
        response.sendRedirect(request.getContextPath() + "/post/detail?id=" + post.getId());
    }

    private boolean ensureLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (WebUtil.getLoginUser(request) != null) {
            return true;
        }

        WebUtil.setFlash(request, "请先登录");
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }

    private String validate(String title, String content) {
        if (title.length() == 0 || title.length() > 100) {
            return "留言主题需为 1-100 个字符";
        }
        if (content.length() == 0 || content.length() > 4000) {
            return "留言内容需为 1-4000 个字符";
        }
        return null;
    }
}
