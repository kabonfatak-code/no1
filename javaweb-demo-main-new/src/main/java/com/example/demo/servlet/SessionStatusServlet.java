package com.example.demo.servlet;

import com.example.demo.model.User;
import com.example.demo.util.WebUtil;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/session/status")
public class SessionStatusServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        User loginUser = WebUtil.getLoginUser(request);
        response.getWriter().write("{\"ok\":true,\"loggedIn\":" + (loginUser != null) + "}");
    }
}
