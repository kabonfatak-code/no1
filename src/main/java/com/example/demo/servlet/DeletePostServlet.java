package com.example.demo.servlet;

import com.example.demo.model.User;
import com.example.demo.repository.BbsRepository;
import com.example.demo.util.WebUtil;

import java.io.IOException;

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

        if (!loginUser.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        long id = parseId(request.getParameter("id"));
        BbsRepository repository = WebUtil.getRepository(getServletContext());
        boolean deleted = repository.deletePost(id);
        WebUtil.setFlash(request, deleted ? "留言已删除" : "留言不存在");
        response.sendRedirect(request.getContextPath() + "/posts");
    }

    private long parseId(String idText) {
        try {
            return Long.parseLong(idText);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
