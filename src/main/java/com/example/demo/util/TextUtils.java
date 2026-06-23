package com.example.demo.util;

public final class TextUtils {
    private TextUtils() {
    }

    public static String trim(String text) {
        return text == null ? "" : text.trim();
    }

    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&#39;");
                    break;
                default:
                    escaped.append(ch);
                    break;
            }
        }
        return escaped.toString();
    }

    public static String escapeHtmlWithLineBreaks(String text) {
        return escapeHtml(text).replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br>");
    }
}
