package com.cotato.nextstation.domain.auth.util;

// signupToken 발급(SignupCommandService)과 검증(ProfileSetupCommandService)이 공유하는 claim 상수
public final class SignupTokenClaims {

    public static final String PURPOSE_KEY = "purpose";
    public static final String SIGNUP_PURPOSE = "SIGNUP";

    private SignupTokenClaims() {
    }
}