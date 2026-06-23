package com.example.demo.servlet;

import com.example.demo.model.User;
import com.example.demo.repository.BbsRepository;
import com.example.demo.util.TextUtils;
import com.example.demo.util.WebUtil;

import java.io.IOException;

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
        request.setCharacterEncoding("UTF-8");

        String username = TextUtils.trim(request.getParameter("username"));
        String password = TextUtils.trim(request.getParameter("password"));
        String realName = TextUtils.trim(request.getParameter("realName"));
        String email = TextUtils.trim(request.getParameter("email"));
        String gender = TextUtils.trim(request.getParameter("gender"));
        String phone = TextUtils.trim(request.getParameter("phone"));

        request.setAttribute("username", username);
        request.setAttribute("realName", realName);
        request.setAttribute("email", email);
        request.setAttribute("gender", gender);
        request.setAttribute("phone", phone);

        String error = validate(username, password, realName, email);
        if (error != null) {
            request.setAttribute("error", error);
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        User user = new User(username, null, realName, email, gender, phone, false);
        BbsRepository repository = WebUtil.getRepository(getServletContext());
        if (!repository.register(user, password)) {
            request.setAttribute("error", "用户名已存在");
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        WebUtil.setFlash(request, "注册成功，请登录");
        response.sendRedirect(request.getContextPath() + "/login");
    }

    private String validate(String username, String password, String realName, String email) {
        if (!username.matches("[A-Za-z0-9_]{3,20}")) {
            return "用户名需为 3-20 位字母、数字或下划线";
        }
        if (password.length() < 4 || password.length() > 32) {
            return "密码长度需为 4-32 位";
        }
        if (realName.length() == 0) {
            return "请输入姓名";
        }
        if (email.length() == 0 || !email.contains("@")) {
            return "请输入有效邮箱";
        }
        return null;
    }
}
