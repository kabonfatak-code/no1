package com.example.demo.servlet;

import com.example.demo.model.User;
import com.example.demo.repository.BbsRepository;
import com.example.demo.util.TextUtils;
import com.example.demo.util.WebUtil;

import java.io.IOException;
import java.sql.SQLException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/admin/action")
public class AdminActionServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = WebUtil.getLoginUser(request);
        if (user == null || !user.isAdmin()) {
            if (isAjax(request)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                writeJson(response, false, "没有权限");
                return;
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = TextUtils.trim(request.getParameter("action"));
        String message = "操作已完成";
        boolean ok = true;
        try {
            BbsRepository repository = WebUtil.getRepository(getServletContext());
            if ("user".equals(action)) {
                long userId = WebUtil.parseLong(request.getParameter("userId"), -1L);
                String banAction = request.getParameter("banAction");
                boolean banned;
                int banDays;
                if (banAction != null && banAction.contains(":")) {
                    String[] parts = banAction.split(":");
                    banned = "1".equals(parts[0]);
                    banDays = Integer.parseInt(parts[1]);
                } else {
                    banned = "1".equals(request.getParameter("banned"));
                    banDays = WebUtil.parseInt(request.getParameter("banDays"), 0);
                }
                repository.adminUpdateUser(
                        user.getId(),
                        userId,
                        request.getParameter("phone"),
                        TextUtils.trim(request.getParameter("role")),
                        banned,
                        banDays
                );
                message = "用户信息已更新";
            } else if ("pin".equals(action)) {
                repository.setPostPinned(WebUtil.parseLong(request.getParameter("postId"), -1L), "1".equals(request.getParameter("pinned")));
                message = "帖子置顶状态已更新";
            } else if ("report".equals(action)) {
                repository.handleReport(
                        TextUtils.trim(request.getParameter("targetType")),
                        WebUtil.parseLong(request.getParameter("targetId"), -1L),
                        user.getId(),
                        WebUtil.parseInt(request.getParameter("banDays"), 0)
                );
                message = "举报已标记处理";
            } else {
                ok = false;
                message = "未知操作";
            }
        } catch (IllegalArgumentException | SQLException e) {
            ok = false;
            message = e.getMessage();
        }

        if (isAjax(request)) {
            writeJson(response, ok, message);
            return;
        }

        WebUtil.setFlash(request, message);
        response.sendRedirect(request.getContextPath() + "/admin");
    }

    private boolean isAjax(HttpServletRequest request) {
        return "fetch".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    private void writeJson(HttpServletResponse response, boolean ok, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"ok\":" + ok + ",\"message\":\"" + jsonEscape(message) + "\"}");
    }

    private String jsonEscape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }
}
