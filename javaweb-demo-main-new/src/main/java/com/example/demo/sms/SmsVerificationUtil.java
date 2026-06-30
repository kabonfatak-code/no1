package com.example.demo.sms;

import com.example.demo.util.TextUtils;

import java.security.SecureRandom;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class SmsVerificationUtil {
    private static final long CODE_VALID_MILLIS = 5 * 60 * 1000L;
    private static final long SEND_INTERVAL_MILLIS = 60 * 1000L;
    private static final int MAX_VERIFY_FAILURES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SmsVerificationUtil() {
    }

    public static String normalizePhone(String phone) {
        return TextUtils.trim(phone).replaceAll("\\s+", "");
    }

    public static void validatePhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("请输入正确的 11 位手机号");
        }
    }

    public static void ensureCanSend(HttpServletRequest request, String purpose) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        Long lastSentAt = asLong(session.getAttribute(key("SENT_AT", purpose)));
        if (lastSentAt == null) {
            return;
        }
        long remaining = SEND_INTERVAL_MILLIS - (System.currentTimeMillis() - lastSentAt);
        if (remaining > 0) {
            long seconds = (remaining + 999L) / 1000L;
            throw new IllegalArgumentException("请等待 " + seconds + " 秒后再获取验证码");
        }
    }

    public static void rememberAliyunRequest(HttpServletRequest request, String phone,
                                             String purpose, String outId) {
        remember(request, phone, purpose, outId, null, false);
    }

    public static String rememberDemoRequest(HttpServletRequest request, String phone, String purpose) {
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        remember(request, phone, purpose, "", code, true);
        return code;
    }

    private static void remember(HttpServletRequest request, String phone, String purpose,
                                 String outId, String demoCode, boolean demo) {
        HttpSession session = request.getSession(true);
        long now = System.currentTimeMillis();
        session.setAttribute(key("PHONE", purpose), phone);
        session.setAttribute(key("OUT_ID", purpose), outId);
        session.setAttribute(key("DEMO_CODE", purpose), demoCode);
        session.setAttribute(key("DEMO", purpose), demo);
        session.setAttribute(key("SENT_AT", purpose), now);
        session.setAttribute(key("FAILURES", purpose), 0);
    }

    public static void verify(HttpServletRequest request, String phone, String code, String purpose) {
        String normalizedPhone = normalizePhone(phone);
        String normalizedCode = TextUtils.trim(code);
        validatePhone(normalizedPhone);
        if (!normalizedCode.matches("^[0-9A-Za-z]{4,8}$")) {
            throw new IllegalArgumentException("请输入正确的短信验证码");
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalArgumentException("请先获取短信验证码");
        }

        String savedPhone = asString(session.getAttribute(key("PHONE", purpose)));
        String outId = asString(session.getAttribute(key("OUT_ID", purpose)));
        String demoCode = asString(session.getAttribute(key("DEMO_CODE", purpose)));
        Boolean demo = asBoolean(session.getAttribute(key("DEMO", purpose)));
        Long sentAt = asLong(session.getAttribute(key("SENT_AT", purpose)));
        Integer failures = asInteger(session.getAttribute(key("FAILURES", purpose)));

        if (savedPhone.isEmpty() || sentAt == null || !savedPhone.equals(normalizedPhone)) {
            throw new IllegalArgumentException("验证码请求无效，请重新获取");
        }
        if (System.currentTimeMillis() - sentAt > CODE_VALID_MILLIS) {
            clearVerification(session, purpose);
            throw new IllegalArgumentException("验证码已过期，请重新获取");
        }
        if (failures != null && failures >= MAX_VERIFY_FAILURES) {
            clearVerification(session, purpose);
            throw new IllegalArgumentException("验证码尝试次数过多，请重新获取");
        }

        boolean passed;
        if (Boolean.TRUE.equals(demo)) {
            passed = !demoCode.isEmpty() && demoCode.equals(normalizedCode);
        } else {
            if (outId.isEmpty()) {
                clearVerification(session, purpose);
                throw new IllegalArgumentException("验证码请求无效，请重新获取");
            }
            try {
                passed = AliyunSmsSender.verifyCode(normalizedPhone, normalizedCode, outId, purpose);
            } catch (SmsSendException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }

        if (!passed) {
            int newFailures = failures == null ? 1 : failures + 1;
            session.setAttribute(key("FAILURES", purpose), newFailures);
            if (newFailures >= MAX_VERIFY_FAILURES) {
                clearVerification(session, purpose);
                throw new IllegalArgumentException("验证码错误次数过多，请重新获取");
            }
            throw new IllegalArgumentException("短信验证码错误");
        }

        clearVerification(session, purpose);
    }

    private static void clearVerification(HttpSession session, String purpose) {
        session.removeAttribute(key("PHONE", purpose));
        session.removeAttribute(key("OUT_ID", purpose));
        session.removeAttribute(key("DEMO_CODE", purpose));
        session.removeAttribute(key("DEMO", purpose));
        session.removeAttribute(key("SENT_AT", purpose));
        session.removeAttribute(key("FAILURES", purpose));
    }

    private static String key(String name, String purpose) {
        return "BBS_SMS_" + name + "_" + normalizePurpose(purpose);
    }

    public static String normalizePurpose(String purpose) {
        String clean = TextUtils.trim(purpose).toUpperCase();
        if (!"REGISTER".equals(clean) && !"LOGIN".equals(clean) && !"RESET".equals(clean)) {
            return "LOGIN";
        }
        return clean;
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Long asLong(Object value) {
        return value instanceof Long ? (Long) value : null;
    }

    private static Integer asInteger(Object value) {
        return value instanceof Integer ? (Integer) value : null;
    }

    private static Boolean asBoolean(Object value) {
        return value instanceof Boolean ? (Boolean) value : Boolean.FALSE;
    }
}
