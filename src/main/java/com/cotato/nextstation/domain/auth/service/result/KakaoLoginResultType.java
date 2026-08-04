package com.cotato.nextstation.domain.auth.service.result;

public enum KakaoLoginResultType {
    NEW_MEMBER,      // Member 미생성, kakaoSignupToken만 발급됨
    PENDING_PROFILE, // Member(PENDING) 존재, signupToken 재발급됨
    LOGIN_SUCCESS    // Member(ACTIVE), access+refresh 발급됨
}