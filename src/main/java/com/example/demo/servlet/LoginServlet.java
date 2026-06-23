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

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String username = TextUtils.trim(request.getParameter("username"));
        String password = TextUtils.trim(request.getParameter("password"));
        BbsRepository repository = WebUtil.getRepository(getServletContext());
        User user = repository.authenticate(username, password);

        if (user == null) {
            request.setAttribute("error", "用户名或密码错误");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }

        request.getSession().setAttribute(WebUtil.LOGIN_USER_KEY, user);
        WebUtil.setFlash(request, "登录成功");
        response.sendRedirect(request.getContextPath() + "/posts");
    }
}
