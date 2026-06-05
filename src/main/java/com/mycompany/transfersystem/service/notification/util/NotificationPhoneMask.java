package com.mycompany.transfersystem.service.notification.util;

public final class NotificationPhoneMask {

    private NotificationPhoneMask() {
    }

    public static String mask(String phone) {
        if (phone == null || phone.isBlank()) {
            return "****";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "****";
        }
        return "****" + digits.substring(digits.length() - 4);
    }

    public static String toE164Digits(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9]", "");
    }
}
