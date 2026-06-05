package com.mycompany.transfersystem.service.notification.provider;

import org.springframework.http.HttpStatusCode;

import java.util.Optional;

public record WhatsAppSendResult(String providerMessageId,
                                 boolean success,
                                 String failureDetail,
                                 Integer httpStatus,
                                 boolean transientFailure) {

    public static WhatsAppSendResult ok(String providerMessageId) {
        return new WhatsAppSendResult(providerMessageId, true, null, 200, false);
    }

    public static WhatsAppSendResult fail(String detail, Integer httpStatus, boolean transientFailure) {
        return new WhatsAppSendResult(null, false, detail, httpStatus, transientFailure);
    }

    public static boolean isTransientHttp(HttpStatusCode status) {
        if (status == null) {
            return true;
        }
        int v = status.value();
        return v == 429 || v >= 500;
    }
}
