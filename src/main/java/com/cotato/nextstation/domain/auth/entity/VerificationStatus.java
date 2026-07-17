package com.cotato.nextstation.domain.auth.entity;

public enum VerificationStatus {
    PENDING, // 인증 대기 중 (코드 발송됨)
    VERIFIED, // 인증 완료
    FAILED, // 인증 실패
    EXPIRED // 코드 만료
}