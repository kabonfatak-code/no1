package com.example.demo.servlet;

import com.example.demo.model.User;
import com.example.demo.repository.BbsRepository;
import com.example.demo.util.WebUtil;

import java.io.IOException;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/post/delete")
public class DeletePostServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User loginUser = WebUtil.getLoginUser(request);
        if (loginUser == null) {
            WebUtil.setFlash(request, "请先登录");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            long id = WebUtil.parseLong(request.getParameter("id"), -1L);
            BbsRepository repository = WebUtil.getRepository(getServletContext());
            repository.deletePost(id, loginUser);
            WebUtil.setFlash(request, "帖子已删除");
            response.sendRedirect(request.getContextPath() + "/posts");
        } catch (IllegalArgumentException | SQLException e) {
            WebUtil.setFlash(request, e.getMessage());
            response.sendRedirect(request.getContextPath() + "/posts");
        }
    }
}
