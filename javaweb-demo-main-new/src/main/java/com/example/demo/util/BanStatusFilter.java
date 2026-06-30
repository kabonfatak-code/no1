package com.example.demo.util;

import com.example.demo.model.User;
import com.example.demo.repository.BbsRepository;

import java.io.IOException;
import java.sql.SQLException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebFilter("/*")
public class BanStatusFilter implements Filter {
    private static final String BANNED_MESSAGE = "账号已被封禁，请联系管理员";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest) || !(servletResponse instanceof HttpServletResponse)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpSession session = request.getSession(false);
        User loginUser = session == null ? null : (User) session.getAttribute(WebUtil.LOGIN_USER_KEY);
        if (loginUser == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            BbsRepository repository = WebUtil.getRepository(request.getServletContext());
            User currentUser = repository.findUserById(loginUser.getId());
            if (currentUser == null || currentUser.isBanned()) {
                session.invalidate();
                writeBannedResponse(request, response);
                return;
            }
            session.setAttribute(WebUtil.LOGIN_USER_KEY, currentUser);
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        chain.doFilter(request, response);
    }

    private void writeBannedResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String redirectUrl = request.getContextPath() + "/login";
        if (expectsJson(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write("{\"ok\":false,\"banned\":true,\"message\":\""
                    + json(BANNED_MESSAGE) + "\",\"redirect\":\"" + json(redirectUrl) + "\"}");
            return;
        }

        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().write("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"><title>账号已封禁</title></head>"
                + "<body><script>alert('" + js(BANNED_MESSAGE) + "');window.location.replace('"
                + js(redirectUrl) + "');</script></body></html>");
    }

    private boolean expectsJson(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        return "fetch".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.toLowerCase().contains("application/json"))
                || uri.endsWith("/session/status");
    }

    private String json(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String js(String text) {
        return json(text).replace("'", "\\'");
    }
}
