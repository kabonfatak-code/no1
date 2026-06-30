package com.example.demo.servlet;

import com.example.demo.repository.BbsRepository;
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

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = TextUtils.trim(request.getParameter("username"));
        String password = TextUtils.trim(request.getParameter("password"));
        String phone = TextUtils.trim(request.getParameter("phone"));
        String smsCode = TextUtils.trim(request.getParameter("smsCode"));

        request.setAttribute("username", username);
        request.setAttribute("phone", phone);

        BbsRepository repository = WebUtil.getRepository(getServletContext());
        try {
            SmsVerificationUtil.verify(request, phone, smsCode, "REGISTER");
            repository.register(username, password, phone);
            WebUtil.setFlash(request, "注册成功，请登录");
            response.sendRedirect(request.getContextPath() + "/login");
        } catch (IllegalArgumentException | SQLException e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        }
    }
}
