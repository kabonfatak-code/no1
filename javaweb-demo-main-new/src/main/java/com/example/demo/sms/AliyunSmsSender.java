package com.example.demo.sms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 阿里云号码认证服务（Dypnsapi）短信验证码客户端。
 *
 * <p>验证码由阿里云动态生成，并通过 CheckSmsVerifyCode 完成校验。
 * AccessKey、签名和模板均从系统属性或环境变量读取，不写死在源码中。</p>
 */
public final class AliyunSmsSender {
    private static final String DEFAULT_ENDPOINT = "https://dypnsapi.aliyuncs.com/";
    private static final String API_VERSION = "2017-05-25";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private AliyunSmsSender() {
    }

    /**
     * 发送由阿里云动态生成的验证码。
     *
     * @return 本次发送的外部流水号，校验验证码时需要原样传回
     */
    public static String sendCode(String phone, String purpose) {
        Config config = Config.load(purpose);
        String outId = UUID.randomUUID().toString().replace("-", "");

        Map<String, String> params = commonParams(config, "SendSmsVerifyCode");
        params.put("CountryCode", "86");
        params.put("PhoneNumber", phone);
        params.put("SignName", config.signName);
        params.put("TemplateCode", config.templateCode);
        params.put("TemplateParam", "{\"code\":\"##code##\",\"min\":\"5\"}");
        params.put("OutId", outId);
        params.put("CodeLength", "6");
        params.put("ValidTime", "300");
        params.put("DuplicatePolicy", "1");
        params.put("Interval", "60");
        params.put("CodeType", "1");
        params.put("ReturnVerifyCode", "false");
        params.put("AutoRetry", "1");
        if (!config.schemeName.isEmpty()) {
            params.put("SchemeName", config.schemeName);
        }

        String response = invoke(config, params);
        ensureApiSuccess(response, "验证码发送失败");
        return outId;
    }

    /**
     * 调用阿里云核验用户输入的短信验证码。
     */
    public static boolean verifyCode(String phone, String code, String outId, String purpose) {
        Config config = Config.load(purpose);
        Map<String, String> params = commonParams(config, "CheckSmsVerifyCode");
        params.put("CountryCode", "86");
        params.put("PhoneNumber", phone);
        params.put("VerifyCode", code);
        params.put("CaseAuthPolicy", "1");
        if (outId != null && !outId.trim().isEmpty()) {
            params.put("OutId", outId.trim());
        }
        if (!config.schemeName.isEmpty()) {
            params.put("SchemeName", config.schemeName);
        }

        String response = invoke(config, params);
        ensureApiSuccess(response, "验证码核验失败");
        return "PASS".equals(readJsonValue(response, "VerifyResult"));
    }

    private static Map<String, String> commonParams(Config config, String action) {
        Map<String, String> params = new TreeMap<>();
        params.put("AccessKeyId", config.accessKeyId);
        params.put("Action", action);
        params.put("Format", "JSON");
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", UUID.randomUUID().toString());
        params.put("SignatureVersion", "1.0");
        params.put("Timestamp", TIMESTAMP_FORMATTER.format(Instant.now()));
        params.put("Version", API_VERSION);
        return params;
    }

    private static String invoke(Config config, Map<String, String> params) {
        try {
            String canonicalizedQuery = canonicalize(params);
            String signature = sign(canonicalizedQuery, config.accessKeySecret);
            String requestUrl = config.endpoint + "?Signature=" + percentEncode(signature) + "&" + canonicalizedQuery;
            return get(requestUrl);
        } catch (IOException | GeneralSecurityException e) {
            throw new SmsSendException("短信服务调用失败：" + safeMessage(e), e);
        }
    }

    private static void ensureApiSuccess(String response, String prefix) {
        String responseCode = readJsonValue(response, "Code");
        if (!"OK".equals(responseCode)) {
            String message = readJsonValue(response, "Message");
            if (message.isEmpty()) {
                message = responseCode.isEmpty() ? "阿里云未返回有效结果" : responseCode;
            }
            throw new SmsSendException(prefix + "：" + message);
        }
    }

    private static String canonicalize(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(percentEncode(entry.getKey()));
            builder.append('=');
            builder.append(percentEncode(entry.getValue()));
        }
        return builder.toString();
    }

    private static String sign(String canonicalizedQuery, String accessKeySecret)
            throws GeneralSecurityException, UnsupportedEncodingException {
        String stringToSign = "GET&%2F&" + percentEncode(canonicalizedQuery);
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
    }

    private static String get(String requestUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(10000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String body = readAll(stream);
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " " + body);
        }
        return body;
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = stream.read(buffer)) != -1) {
            output.write(buffer, 0, length);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String readJsonValue(String json, String key) {
        if (json == null) {
            return "";
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String percentEncode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8")
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? exception.getClass().getSimpleName() : message;
    }

    private static final class Config {
        private final String accessKeyId;
        private final String accessKeySecret;
        private final String signName;
        private final String templateCode;
        private final String schemeName;
        private final String endpoint;

        private Config(String accessKeyId, String accessKeySecret, String signName,
                       String templateCode, String schemeName, String endpoint) {
            this.accessKeyId = accessKeyId;
            this.accessKeySecret = accessKeySecret;
            this.signName = signName;
            this.templateCode = templateCode;
            this.schemeName = schemeName;
            this.endpoint = endpoint.endsWith("/") ? endpoint : endpoint + "/";
        }

        private static Config load(String purpose) {
            String purposeKey = purpose == null ? "LOGIN" : purpose.trim().toUpperCase();
            String specificTemplate = firstNonBlank(
                    System.getProperty("bbs.sms.aliyun.template." + purposeKey.toLowerCase()),
                    System.getenv("BBS_SMS_ALIYUN_TEMPLATE_" + purposeKey),
                    System.getenv("SMS_TEMPLATE_" + purposeKey)
            );
            String commonTemplate = firstNonBlank(
                    System.getProperty("bbs.sms.aliyun.templateCode"),
                    System.getenv("BBS_SMS_ALIYUN_TEMPLATE_CODE"),
                    System.getenv("SMS_TEMPLATE_CODE")
            );

            return new Config(
                    requireValue(firstNonBlank(
                            System.getProperty("bbs.sms.aliyun.accessKeyId"),
                            System.getenv("BBS_SMS_ALIYUN_ACCESS_KEY_ID"),
                            System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID")
                    ), "阿里云 AccessKeyId"),
                    requireValue(firstNonBlank(
                            System.getProperty("bbs.sms.aliyun.accessKeySecret"),
                            System.getenv("BBS_SMS_ALIYUN_ACCESS_KEY_SECRET"),
                            System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET")
                    ), "阿里云 AccessKeySecret"),
                    requireValue(firstNonBlank(
                            System.getProperty("bbs.sms.aliyun.signName"),
                            System.getenv("BBS_SMS_ALIYUN_SIGN_NAME"),
                            System.getenv("SMS_SIGN_NAME")
                    ), "阿里云短信签名"),
                    requireValue(specificTemplate.isEmpty() ? commonTemplate : specificTemplate,
                            "阿里云短信模板 Code"),
                    firstNonBlank(
                            System.getProperty("bbs.sms.aliyun.schemeName"),
                            System.getenv("BBS_SMS_ALIYUN_SCHEME_NAME"),
                            System.getenv("SMS_SCHEME_NAME")
                    ),
                    defaultIfBlank(firstNonBlank(
                            System.getProperty("bbs.sms.aliyun.endpoint"),
                            System.getenv("BBS_SMS_ALIYUN_ENDPOINT")
                    ), DEFAULT_ENDPOINT)
            );
        }

        private static String firstNonBlank(String... values) {
            if (values != null) {
                for (String value : values) {
                    if (value != null && !value.trim().isEmpty()) {
                        return value.trim();
                    }
                }
            }
            return "";
        }

        private static String requireValue(String value, String label) {
            String clean = value == null ? "" : value.trim();
            if (clean.isEmpty()) {
                throw new SmsSendException("短信服务未配置：" + label);
            }
            return clean;
        }

        private static String defaultIfBlank(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value.trim();
        }
    }
}
