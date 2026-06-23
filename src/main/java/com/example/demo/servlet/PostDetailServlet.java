package com.example.demo.servlet;

import com.example.demo.model.Post;
import com.example.demo.repository.BbsRepository;
import com.example.demo.util.WebUtil;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/post/detail")
public class PostDetailServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long id = parseId(request.getParameter("id"));
        BbsRepository repository = WebUtil.getRepository(getServletContext());
        Post post = repository.findPost(id);

        if (post == null) {
            WebUtil.setFlash(request, "留言不存在或已删除");
            response.sendRedirect(request.getContextPath() + "/posts");
            return;
        }

        request.setAttribute("post", post);
        request.getRequestDispatcher("/WEB-INF/views/detail.jsp").forward(request, response);
    }

    private long parseId(String idText) {
        try {
            return Long.parseLong(idText);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
