package com.mycompany.transfersystem.service;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

    private static final int SECRET_SIZE = 160;
    private static final int CODE_DIGITS = 6;
    private static final int TIME_PERIOD = 30;

    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator(SECRET_SIZE);
    private final DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, CODE_DIGITS);
    private final TimeProvider timeProvider = new SystemTimeProvider();

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public boolean verifyCode(String secret, String code, int windowTolerance) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }
        try {
            long currentBucket = timeProvider.getTime() / TIME_PERIOD;
            for (int i = -windowTolerance; i <= windowTolerance; i++) {
                String expectedCode = codeGenerator.generate(secret, currentBucket + i);
                if (expectedCode.equals(code)) {
                    return true;
                }
            }
        } catch (dev.samstevens.totp.exceptions.CodeGenerationException e) {
            return false;
        }
        return false;
    }
}
