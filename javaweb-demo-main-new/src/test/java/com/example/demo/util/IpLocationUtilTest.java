package com.example.demo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpLocationUtilTest {
    @Test
    void resolvesProvinceFromCommonIpApiFields() {
        assertEquals("福建", IpLocationUtil.provinceFromJson("{\"regionName\":\"Fujian\"}"));
        assertEquals("福建", IpLocationUtil.provinceFromJson("{\"province\":\"福建省\"}"));
        assertEquals("福建", IpLocationUtil.provinceFromJson("{\"pro\":\"福建省\",\"city\":\"福州市\"}"));
        assertEquals("福建", IpLocationUtil.provinceFromJson("{\"data\":{\"prov\":\"福建省\"}}"));
        assertEquals("福建", IpLocationUtil.provinceFromJson("{\"addr\":\"中国 福建省 福州市\"}"));
    }

    @Test
    void resolvesProvinceFromIp2regionRecordText() {
        assertEquals("福建", IpLocationUtil.provinceFromRegionText("中国|0|福建省|福州市|移动"));
    }

    @Test
    void resolvesProvinceFromBundledIp2regionXdbWithoutOnlineLookup() {
        String previous = System.getProperty("bbs.ip.location.enabled");
        System.setProperty("bbs.ip.location.enabled", "false");
        try {
            assertEquals("福建", IpLocationUtil.resolveProvince("112.51.189.189"));
        } finally {
            if (previous == null) {
                System.clearProperty("bbs.ip.location.enabled");
            } else {
                System.setProperty("bbs.ip.location.enabled", previous);
            }
        }
    }
}
