package com.example.demo.servlet;

import com.example.demo.sms.SmsVerificationUtil;
import com.example.demo.util.TextUtils;
import com.example.demo.util.WebUtil;

import java.io.IOException;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/password/reset")
public class PasswordResetServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String phone = TextUtils.trim(request.getParameter("phone"));
        String smsCode = TextUtils.trim(request.getParameter("smsCode"));
        String password = TextUtils.trim(request.getParameter("password"));

        request.setAttribute("phone", phone);
        try {
            SmsVerificationUtil.verify(request, phone, smsCode, "RESET");
            WebUtil.getRepository(getServletContext()).resetPassword(phone, password);
            WebUtil.setFlash(request, "密码已重置，请登录");
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (IllegalArgumentException | SQLException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(request, response);
        }
    }
}
