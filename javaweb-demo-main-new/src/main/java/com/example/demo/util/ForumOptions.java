package com.example.demo.util;

import com.example.demo.model.User;

public final class ForumOptions {
    public static final String[] PROVINCES = {
            "北京", "天津", "河北", "山西", "内蒙古", "辽宁", "吉林", "黑龙江",
            "上海", "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南",
            "湖北", "湖南", "广东", "广西", "海南", "重庆", "四川", "贵州",
            "云南", "西藏", "陕西", "甘肃", "青海", "宁夏", "新疆", "香港",
            "澳门", "台湾"
    };

    private ForumOptions() {
    }

    public static String normalizeProvince(String value) {
        if (value == null) {
            return "";
        }
        String rawValue = value.trim();
        String cleanValue = rawValue.replace("省", "").replace("市", "");
        for (String province : PROVINCES) {
            if (province.equals(cleanValue) || rawValue.startsWith(province) || cleanValue.contains(province)) {
                return province;
            }
        }
        return englishProvince(cleanValue);
    }

    private static String englishProvince(String value) {
        if (value.endsWith(" province")) {
            value = value.substring(0, value.length() - " province".length()).trim();
        }
        switch (value.toLowerCase()) {
            case "beijing": return "北京";
            case "tianjin": return "天津";
            case "hebei": return "河北";
            case "shanxi": return "山西";
            case "inner mongolia": return "内蒙古";
            case "liaoning": return "辽宁";
            case "jilin": return "吉林";
            case "heilongjiang": return "黑龙江";
            case "shanghai": return "上海";
            case "jiangsu": return "江苏";
            case "zhejiang": return "浙江";
            case "anhui": return "安徽";
            case "fujian": return "福建";
            case "jiangxi": return "江西";
            case "shandong": return "山东";
            case "henan": return "河南";
            case "hubei": return "湖北";
            case "hunan": return "湖南";
            case "guangdong": return "广东";
            case "guangxi": return "广西";
            case "hainan": return "海南";
            case "chongqing": return "重庆";
            case "sichuan": return "四川";
            case "guizhou": return "贵州";
            case "yunnan": return "云南";
            case "tibet":
            case "xizang": return "西藏";
            case "shaanxi":
            case "shensi": return "陕西";
            case "gansu": return "甘肃";
            case "qinghai": return "青海";
            case "ningxia": return "宁夏";
            case "xinjiang": return "新疆";
            case "hong kong": return "香港";
            case "macau":
            case "macao": return "澳门";
            case "taiwan": return "台湾";
            default: return "";
        }
    }

    public static String roleLabel(String role) {
        if (User.ROLE_ADMIN.equals(role)) {
            return "系统管理员";
        }
        if (User.ROLE_OLD.equals(role)) {
            return "老东西";
        }
        return "新用户";
    }
}
