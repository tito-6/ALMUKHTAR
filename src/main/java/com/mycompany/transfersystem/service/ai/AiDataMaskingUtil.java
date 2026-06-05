package com.mycompany.transfersystem.service.ai;

import java.util.regex.Pattern;

public final class AiDataMaskingUtil {

    private static final Pattern PHONE = Pattern.compile("\\+?\\d[\\d\\s\\-()]{7,}\\d");
    private static final Pattern LONG_DIGITS = Pattern.compile("\\b\\d{10,}\\b");

    private AiDataMaskingUtil() {}

    public static String maskPhonesAndIds(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String s = PHONE.matcher(text).replaceAll("***PHONE***");
        return LONG_DIGITS.matcher(s).replaceAll("***ID***");
    }
}
