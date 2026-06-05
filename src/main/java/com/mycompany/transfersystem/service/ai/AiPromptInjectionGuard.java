package com.mycompany.transfersystem.service.ai;

import org.springframework.util.StringUtils;

public final class AiPromptInjectionGuard {

    private AiPromptInjectionGuard() {}

    public static String sanitizeUserMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        String m = message.strip();
        String lower = m.toLowerCase();
        if (containsBlockedInstruction(lower)) {
            return "[Message removed by safety policy: do not request system prompts, credentials, SQL, or other users' data.]";
        }
        return m;
    }

    private static boolean containsBlockedInstruction(String lower) {
        return lower.contains("ignore previous")
                || lower.contains("ignore all previous")
                || lower.contains("system prompt")
                || lower.contains("developer message")
                || lower.contains("you are now")
                || lower.contains("reveal your")
                || lower.contains("xi-api-key")
                || lower.contains("jwt secret")
                || (lower.contains("password") && lower.contains("database"))
                || (lower.contains("select ") && lower.contains(" from "))
                || lower.contains("drop table")
                || lower.contains("other user")
                || lower.contains("someone else's");
    }
}
