package com.example.demo.servlet;

import com.example.demo.model.User;
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

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String mode = TextUtils.trim(request.getParameter("mode"));
        try {
            BbsRepository repository = WebUtil.getRepository(getServletContext());
            User user;
            if ("sms".equals(mode)) {
                String phone = request.getParameter("phone");
                SmsVerificationUtil.verify(request, phone, request.getParameter("smsCode"), "LOGIN");
                user = repository.authenticateByPhone(phone);
            } else {
                user = repository.authenticateByPassword(request.getParameter("username"), request.getParameter("password"));
            }

            if (user == null) {
                request.setAttribute("error", "sms".equals(mode) ? "该手机号尚未注册" : "用户名或密码错误");
                request.setAttribute("username", TextUtils.trim(request.getParameter("username")));
                request.setAttribute("phone", TextUtils.trim(request.getParameter("phone")));
                request.setAttribute("mode", mode);
                request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                return;
            }
            if (user.isBanned()) {
                request.setAttribute("error", "账号已被封禁，请联系管理员");
                request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                return;
            }

            request.getSession().setAttribute(WebUtil.LOGIN_USER_KEY, user);
            WebUtil.setFlash(request, "登录成功");
            response.sendRedirect(request.getContextPath() + "/posts");
        } catch (IllegalArgumentException | SQLException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("mode", mode);
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        }
    }
}
