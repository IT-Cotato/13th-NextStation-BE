package com.cotato.nextstation.domain.auth.util;

public final class EmailMasker {

    private static final int VISIBLE_LOCAL_LENGTH = 2;
    private static final String MASKED = "***";

    private EmailMasker() {
    }

    public static String mask(String email) {
        if (email == null || email.isBlank()) {
            return MASKED;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return MASKED;
        }

        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        int visibleLength = Math.min(VISIBLE_LOCAL_LENGTH, local.length());
        return local.substring(0, visibleLength) + MASKED + domain;
    }
}