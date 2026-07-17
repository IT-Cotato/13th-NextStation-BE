package com.cotato.nextstation.domain.auth.util;

import java.security.SecureRandom;

public class VerificationCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final int CODE_BOUND = 1_000_000; // 6자리(000000~999999)

    private VerificationCodeGenerator() {
    }

    public static String generate() {
        int code = RANDOM.nextInt(CODE_BOUND);
        return String.format("%0" + CODE_LENGTH + "d", code);
    }
}