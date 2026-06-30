package com.example.demo.servlet;

import com.example.demo.model.Post;
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

@WebServlet("/post/detail")
public class PostDetailServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            long id = WebUtil.parseLong(request.getParameter("id"), -1L);
            BbsRepository repository = WebUtil.getRepository(getServletContext());
            Post post = repository.findPost(id);

            if (post == null) {
                WebUtil.setFlash(request, "帖子不存在或已删除");
                response.sendRedirect(request.getContextPath() + "/posts");
                return;
            }

            User user = WebUtil.getLoginUser(request);
            repository.addHistory(user, id);
            request.setAttribute("post", post);
            request.setAttribute("comments", repository.findComments(id));
            request.getRequestDispatcher("/WEB-INF/views/detail.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
