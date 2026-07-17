package com.cotato.nextstation.domain.auth.entity;

import com.cotato.nextstation.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "email_verification",
        indexes = {
                @Index(name = "idx_email_verification_email", columnList = "email"),
                @Index(name = "idx_email_verification_expires_at", columnList = "expires_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseTimeEntity {

    // 회원가입 인증 시점엔 아직 Member가 생성되기 전이라 FK 대신 nullable 스칼라 값으로 둔다.
    @Column(name = "member_id")
    private Long memberId;

    @Column(nullable = false)
    private String email;

    @Column(name = "verification_code", nullable = false, length = 6)
    private String verificationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Builder
    private EmailVerification(Long memberId, String email, String verificationCode, VerificationType type, LocalDateTime expiresAt) {
        this.memberId = memberId;
        this.email = email;
        this.verificationCode = verificationCode;
        this.type = type;
        this.status = VerificationStatus.PENDING;
        this.attemptCount = 0;
        this.expiresAt = expiresAt;
    }

    public void verify() {
        this.status = VerificationStatus.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
    }

    public void increaseAttemptCount() {
        this.attemptCount++;
    }

    public void fail() {
        this.status = VerificationStatus.FAILED;
        this.failedAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = VerificationStatus.EXPIRED;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}