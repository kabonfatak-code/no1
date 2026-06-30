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

@WebServlet("/admin")
public class AdminServlet extends HttpServlet {
    private static final int PAGE_SIZE = 8;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = WebUtil.getLoginUser(request);
        if (user == null || !user.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            BbsRepository repository = WebUtil.getRepository(getServletContext());
            int reportTotalCount = repository.countReports();
            int reportTotalPages = Math.max(1, (reportTotalCount + PAGE_SIZE - 1) / PAGE_SIZE);
            int reportPage = Math.min(Math.max(WebUtil.parseInt(request.getParameter("reportPage"), WebUtil.parseInt(request.getParameter("page"), 1)), 1), reportTotalPages);
            int userTotalCount = repository.countUsers();
            int userTotalPages = Math.max(1, (userTotalCount + PAGE_SIZE - 1) / PAGE_SIZE);
            int userPage = Math.min(Math.max(WebUtil.parseInt(request.getParameter("userPage"), 1), 1), userTotalPages);

            request.setAttribute("users", repository.findUsers(userPage, PAGE_SIZE));
            request.setAttribute("reports", repository.findReports(reportPage, PAGE_SIZE));
            request.setAttribute("reportCurrentPage", reportPage);
            request.setAttribute("reportTotalPages", reportTotalPages);
            request.setAttribute("reportTotalCount", reportTotalCount);
            request.setAttribute("userCurrentPage", userPage);
            request.setAttribute("userTotalPages", userTotalPages);
            request.setAttribute("userTotalCount", userTotalCount);
            request.getRequestDispatcher("/WEB-INF/views/admin.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
