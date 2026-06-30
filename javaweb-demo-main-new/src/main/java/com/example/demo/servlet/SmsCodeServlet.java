package com.example.demo.servlet;

import com.example.demo.sms.AliyunSmsSender;
import com.example.demo.sms.SmsSendException;
import com.example.demo.sms.SmsVerificationUtil;
import com.example.demo.util.TextUtils;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/sms-code")
public class SmsCodeServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json; charset=UTF-8");
        String normalizedPhone = SmsVerificationUtil.normalizePhone(request.getParameter("phone"));
        String purpose = normalizePurpose(request.getParameter("purpose"));

        try {
            SmsVerificationUtil.validatePhone(normalizedPhone);
            SmsVerificationUtil.ensureCanSend(request, purpose);

            if (isDemoSmsEnabled()) {
                String code = SmsVerificationUtil.rememberDemoRequest(request, normalizedPhone, purpose);
                response.getWriter().write("{\"ok\":true,\"code\":\"" + code
                        + "\",\"message\":\"演示验证码：" + code + "\"}");
                return;
            }

            String outId = AliyunSmsSender.sendCode(normalizedPhone, purpose);
            SmsVerificationUtil.rememberAliyunRequest(request, normalizedPhone, purpose, outId);
            response.getWriter().write("{\"ok\":true,\"message\":\"验证码已发送，请查收短信\"}");
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"ok\":false,\"message\":\"" + json(e.getMessage()) + "\"}");
        } catch (SmsSendException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"ok\":false,\"message\":\"" + json(e.getMessage()) + "\"}");
        }
    }

    private String normalizePurpose(String purpose) {
        String clean = TextUtils.trim(purpose).toUpperCase();
        if (!"REGISTER".equals(clean) && !"LOGIN".equals(clean) && !"RESET".equals(clean)) {
            return "LOGIN";
        }
        return clean;
    }

    private String json(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean isDemoSmsEnabled() {
        String property = System.getProperty("bbs.sms.demo");
        if (property == null || property.trim().isEmpty()) {
            property = System.getenv("BBS_SMS_DEMO");
        }
        return "true".equalsIgnoreCase(property) || "1".equals(property);
    }
}
