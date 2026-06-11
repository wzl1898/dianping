package com.dianping.utils;

import cn.hutool.core.util.StrUtil;

public class RegexUtils {
    public static boolean isPhoneInvalid(String phone) {
        return !phone.matches("^1[3-9]\\d{9}$");
    }

    public static boolean isValidCode(String code) {
        return StrUtil.isNotBlank(code) && code.matches("\\d{6}");
    }
}
