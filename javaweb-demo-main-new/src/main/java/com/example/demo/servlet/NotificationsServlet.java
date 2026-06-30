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

@WebServlet("/notifications")
public class NotificationsServlet extends HttpServlet {
    private static final int PAGE_SIZE = 8;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = WebUtil.getLoginUser(request);
        if (user == null) {
            WebUtil.setFlash(request, "请先登录");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try {
            BbsRepository repository = WebUtil.getRepository(getServletContext());
            int totalCount = repository.countNotifications(user.getId());
            int totalPages = Math.max(1, (totalCount + PAGE_SIZE - 1) / PAGE_SIZE);
            int page = Math.min(Math.max(WebUtil.parseInt(request.getParameter("page"), 1), 1), totalPages);

            request.setAttribute("notifications", repository.findNotifications(user.getId(), page, PAGE_SIZE));
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("totalCount", totalCount);
            request.getRequestDispatcher("/WEB-INF/views/notifications.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
