package com.example.demo.servlet;

import com.example.demo.repository.BbsRepository;
import com.example.demo.util.WebUtil;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/posts")
public class PostListServlet extends HttpServlet {
    private static final int PAGE_SIZE = 5;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BbsRepository repository = WebUtil.getRepository(getServletContext());
        int totalCount = repository.countPosts();
        int totalPages = Math.max(1, (totalCount + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = parsePage(request.getParameter("page"), totalPages);

        request.setAttribute("posts", repository.findPosts(page, PAGE_SIZE));
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalCount);
        request.getRequestDispatcher("/WEB-INF/views/list.jsp").forward(request, response);
    }

    private int parsePage(String pageText, int totalPages) {
        try {
            int page = Integer.parseInt(pageText);
            if (page < 1) {
                return 1;
            }
            return Math.min(page, totalPages);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
