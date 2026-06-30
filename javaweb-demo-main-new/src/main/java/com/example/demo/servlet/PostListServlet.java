package com.example.demo.servlet;

import com.example.demo.model.PostSearchCriteria;
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

@WebServlet("/posts")
public class PostListServlet extends HttpServlet {
    private static final int PAGE_SIZE = 8;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            BbsRepository repository = WebUtil.getRepository(getServletContext());
            PostSearchCriteria criteria = buildCriteria(request);
            int totalCount = repository.countPosts(criteria);
            int totalPages = Math.max(1, (totalCount + PAGE_SIZE - 1) / PAGE_SIZE);
            int page = Math.min(Math.max(WebUtil.parseInt(request.getParameter("page"), 1), 1), totalPages);

            request.setAttribute("posts", repository.searchPosts(criteria, page, PAGE_SIZE));
            request.setAttribute("topics", repository.findTopics());
            request.setAttribute("criteria", criteria);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("totalCount", totalCount);
            request.getRequestDispatcher("/WEB-INF/views/list.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private PostSearchCriteria buildCriteria(HttpServletRequest request) {
        PostSearchCriteria criteria = new PostSearchCriteria();
        criteria.setTopic(TextUtils.trim(request.getParameter("topic")));
        criteria.setKeyword(TextUtils.trim(request.getParameter("keyword")));
        criteria.setDays(WebUtil.parseInt(request.getParameter("days"), 0));
        criteria.setMinLikes(WebUtil.parseInt(request.getParameter("minLikes"), 0));
        criteria.setMinFavorites(WebUtil.parseInt(request.getParameter("minFavorites"), 0));
        criteria.setOrderBy(TextUtils.trim(request.getParameter("orderBy")));
        return criteria;
    }
}
