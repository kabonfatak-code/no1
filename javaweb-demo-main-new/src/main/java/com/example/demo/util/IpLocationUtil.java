package com.example.demo.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;

public final class IpLocationUtil {
    public static final String LOCAL_REGION = "本地";
    public static final String UNKNOWN_REGION = "未知";

    private static final int TIMEOUT_MS = 800;
    private static final String XDB_RESOURCE = "ip2region/ip2region.xdb";
    private static final String[] DEFAULT_APIS = {
            "https://qifu-api.baidubce.com/ip/geo/v1/district?ip=%s",
            "http://ip-api.com/json/%s?fields=status,country,regionName,region,message&lang=zh-CN",
            "https://whois.pconline.com.cn/ipJson.jsp?ip=%s&json=true"
    };
    private static final Pattern JSON_VALUE = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();
    private static volatile Ip2Region offlineSearcher;
    private static volatile boolean offlineSearcherFailed;

    private IpLocationUtil() {
    }

    public static String resolveProvince(String ip) {
        String cleanIp = TextUtils.trim(ip);
        if (cleanIp.isEmpty()) {
            return UNKNOWN_REGION;
        }
        String cached = CACHE.get(cleanIp);
        if (cached != null) {
            return cached;
        }
        String province = lookupProvince(cleanIp);
        if (!UNKNOWN_REGION.equals(province)) {
            CACHE.put(cleanIp, province);
        }
        return province;
    }

    private static String lookupProvince(String ip) {
        try {
            if (isLocalOrPrivate(ip)) {
                return LOCAL_REGION;
            }

            String province = lookupByXdb(ip);
            if (!province.isEmpty()) {
                return province;
            }

            province = lookupByApi(ip);
            return province.isEmpty() ? UNKNOWN_REGION : province;
        } catch (RuntimeException e) {
            return UNKNOWN_REGION;
        }
    }

    private static String lookupByXdb(String ip) {
        Ip2Region searcher = offlineSearcher();
        if (searcher == null) {
            return "";
        }
        try {
            return provinceFromRegionText(searcher.search(ip));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String lookupByApi(String ip) {
        if (!locationLookupEnabled()) {
            return "";
        }
        String encodedIp = urlEncode(TextUtils.trim(ip));
        for (String api : apiTemplates()) {
            String url = api.contains("%s") ? String.format(api, encodedIp) : api;
            if (encodedIp.isEmpty()) {
                url = url.replace("/%s", "").replace("%s", "");
            }

            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(timeoutMs());
                connection.setReadTimeout(timeoutMs());
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) {
                    continue;
                }
                String province = provinceFromJson(readAll(connection));
                if (!province.isEmpty() && !UNKNOWN_REGION.equals(province)) {
                    return province;
                }
            } catch (IOException | IllegalArgumentException e) {
                // Try the next configured lookup service. Some public APIs are unstable on cloud hosts.
            }
        }
        return "";
    }

    private static String readAll(HttpURLConnection connection) throws IOException {
        InputStream stream = connection.getInputStream();
        if (stream == null) {
            return "";
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = stream.read(buffer)) != -1) {
            output.write(buffer, 0, length);
        }
        return new String(output.toByteArray(), responseCharset(connection.getContentType()));
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (IOException e) {
            return "";
        }
    }

    static String provinceFromJson(String json) {
        String[] provinceKeys = {
                "regionName", "province", "prov", "pro", "region", "info1", "addr", "address"
        };
        for (String key : provinceKeys) {
            String province = ForumOptions.normalizeProvince(readJsonString(json, key));
            if (!province.isEmpty()) {
                return province;
            }
        }

        String province = ForumOptions.normalizeProvince(json);
        if (!province.isEmpty()) {
            return province;
        }

        String country = readJsonString(json, "country");
        return "中国".equals(country) ? UNKNOWN_REGION : "";
    }

    static String provinceFromRegionText(String text) {
        return ForumOptions.normalizeProvince(text);
    }

    private static String readJsonString(String json, String key) {
        Matcher matcher = Pattern.compile(String.format(JSON_VALUE.pattern(), Pattern.quote(key))).matcher(json == null ? "" : json);
        if (!matcher.find()) {
            return "";
        }
        return unescapeJson(matcher.group(1));
    }

    private static String unescapeJson(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '\\' && i + 1 < text.length()) {
                char next = text.charAt(++i);
                if (next == 'u' && i + 4 < text.length()) {
                    String hex = text.substring(i + 1, i + 5);
                    try {
                        result.append((char) Integer.parseInt(hex, 16));
                        i += 4;
                    } catch (NumberFormatException e) {
                        result.append("\\u").append(hex);
                        i += 4;
                    }
                } else {
                    result.append(next);
                }
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }

    private static boolean isLocalOrPrivate(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isLinkLocalAddress() || address.isSiteLocalAddress()) {
                return true;
            }
        } catch (IOException e) {
            return false;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            return first == 10
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168)
                    || (first == 100 && second >= 64 && second <= 127)
                    || (first == 198 && (second == 18 || second == 19));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static int timeoutMs() {
        String configured = System.getProperty("bbs.ip.location.timeout.ms");
        if (configured == null || configured.trim().isEmpty()) {
            return TIMEOUT_MS;
        }
        try {
            return Math.max(300, Integer.parseInt(configured.trim()));
        } catch (NumberFormatException e) {
            return TIMEOUT_MS;
        }
    }

    private static boolean locationLookupEnabled() {
        String configured = System.getProperty("bbs.ip.location.enabled");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv("BBS_IP_LOCATION_ENABLED");
        }
        return configured == null || configured.trim().isEmpty()
                || "true".equalsIgnoreCase(configured)
                || "1".equals(configured.trim());
    }

    private static Ip2Region offlineSearcher() {
        Ip2Region current = offlineSearcher;
        if (current != null || offlineSearcherFailed) {
            return current;
        }
        synchronized (IpLocationUtil.class) {
            current = offlineSearcher;
            if (current != null || offlineSearcherFailed) {
                return current;
            }
            try {
                offlineSearcher = createOfflineSearcher();
                return offlineSearcher;
            } catch (Exception e) {
                offlineSearcherFailed = true;
                return null;
            }
        }
    }

    private static Ip2Region createOfflineSearcher() throws Exception {
        String configuredPath = System.getProperty("bbs.ip.location.xdb.path");
        if (configuredPath != null && !configuredPath.trim().isEmpty()) {
            File file = new File(configuredPath.trim());
            if (file.isFile()) {
                Config config = Config.custom()
                        .setXdbFile(file)
                        .setCachePolicy(Config.BufferCache)
                        .setSearchers(16)
                        .asV4();
                return Ip2Region.create(config, null);
            }
        }

        InputStream stream = IpLocationUtil.class.getClassLoader().getResourceAsStream(XDB_RESOURCE);
        if (stream == null) {
            throw new IOException("ip2region xdb resource not found: " + XDB_RESOURCE);
        }
        try {
            Config config = Config.custom()
                    .setXdbInputStream(stream)
                    .setCachePolicy(Config.BufferCache)
                    .setSearchers(16)
                    .asV4();
            return Ip2Region.create(config, null);
        } finally {
            stream.close();
        }
    }

    private static List<String> apiTemplates() {
        String configured = System.getProperty("bbs.ip.location.api");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getProperty("bbs.ip.location.apis");
        }

        List<String> apis = new ArrayList<>();
        if (configured != null && !configured.trim().isEmpty()) {
            for (String api : configured.split("[,\\n]")) {
                String cleanApi = api.trim();
                if (!cleanApi.isEmpty()) {
                    apis.add(cleanApi);
                }
            }
        }
        if (apis.isEmpty()) {
            for (String api : DEFAULT_APIS) {
                apis.add(api);
            }
        }
        return apis;
    }

    private static Charset responseCharset(String contentType) {
        if (contentType == null) {
            return StandardCharsets.UTF_8;
        }
        String[] parts = contentType.split(";");
        for (String part : parts) {
            String value = part.trim();
            if (value.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                try {
                    return Charset.forName(value.substring("charset=".length()).trim());
                } catch (IllegalArgumentException e) {
                    return StandardCharsets.UTF_8;
                }
            }
        }
        return StandardCharsets.UTF_8;
    }
}
