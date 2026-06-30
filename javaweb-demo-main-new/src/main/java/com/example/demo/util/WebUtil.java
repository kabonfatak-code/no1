package com.example.demo.util;

import com.example.demo.model.User;
import com.example.demo.repository.BbsRepository;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class WebUtil {
    public static final String REPOSITORY_KEY = "bbsRepository";
    public static final String LOGIN_USER_KEY = "loginUser";
    public static final String FLASH_KEY = "flashMessage";

    private WebUtil() {
    }

    public static BbsRepository getRepository(ServletContext context) {
        synchronized (context) {
            BbsRepository repository = (BbsRepository) context.getAttribute(REPOSITORY_KEY);
            if (repository == null) {
                repository = new BbsRepository();
                context.setAttribute(REPOSITORY_KEY, repository);
            }
            return repository;
        }
    }

    public static User getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute(LOGIN_USER_KEY);
    }

    public static boolean isLoggedIn(HttpServletRequest request) {
        return getLoginUser(request) != null;
    }

    public static boolean isAdmin(HttpServletRequest request) {
        User user = getLoginUser(request);
        return user != null && user.isAdmin();
    }

    public static long parseLong(String text, long defaultValue) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int parseInt(String text, int defaultValue) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static void setFlash(HttpServletRequest request, String message) {
        request.getSession().setAttribute(FLASH_KEY, message);
    }

    public static String consumeFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        String message = (String) session.getAttribute(FLASH_KEY);
        session.removeAttribute(FLASH_KEY);
        return message;
    }

    public static String getClientIp(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Ali-CDN-Real-IP",
                "X-Client-IP",
                "X-Cluster-Client-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };
        for (String headerName : headerNames) {
            String value = firstIp(request.getHeader(headerName));
            if (!value.isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                return value;
            }
        }
        String forwardedIp = forwardedIp(request.getHeader("Forwarded"));
        if (!forwardedIp.isEmpty()) {
            return forwardedIp;
        }
        String remoteAddr = request.getRemoteAddr();
        return normalizeIp(remoteAddr == null ? "" : remoteAddr);
    }

    public static String getClientProvince(HttpServletRequest request) {
        String[] headerNames = {
                "X-Client-Province",
                "X-Province",
                "X-GeoIP-Region",
                "X-GeoIP-Province"
        };
        for (String headerName : headerNames) {
            String province = ForumOptions.normalizeProvince(request.getHeader(headerName));
            if (!province.isEmpty()) {
                return province;
            }
        }
        return IpLocationUtil.resolveProvince(getClientIp(request));
    }

    private static String firstIp(String text) {
        if (text == null) {
            return "";
        }
        String[] values = text.split(",");
        for (String rawValue : values) {
            String value = normalizeIp(rawValue);
            if (!value.isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return "";
    }

    private static String forwardedIp(String text) {
        if (text == null) {
            return "";
        }
        String[] items = text.split(",");
        for (String item : items) {
            String[] parts = item.split(";");
            for (String part : parts) {
                String value = part.trim();
                if (value.toLowerCase().startsWith("for=")) {
                    value = value.substring(4).trim();
                    if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (value.startsWith("[") && value.contains("]")) {
                        value = value.substring(1, value.indexOf(']'));
                    }
                    int portIndex = value.lastIndexOf(':');
                    if (portIndex > 0 && value.indexOf(':') == portIndex) {
                        value = value.substring(0, portIndex);
                    }
                    return normalizeIp(value);
                }
            }
        }
        return "";
    }

    private static String normalizeIp(String ip) {
        String value = ip == null ? "" : ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(value) || "::1".equals(value)) {
            return "127.0.0.1";
        }
        return value;
    }

}
