package com.cotato.nextstation.domain.auth.service;

import com.cotato.nextstation.domain.auth.entity.EmailVerification;
import com.cotato.nextstation.domain.auth.entity.VerificationStatus;
import com.cotato.nextstation.domain.auth.entity.VerificationType;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRateLimitRepository;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRepository;
import com.cotato.nextstation.domain.auth.util.EmailMasker;
import com.cotato.nextstation.domain.auth.util.VerificationCodeGenerator;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 인증번호 DB/Redis 쓰기 전용 컴포넌트
 * 메일 발송(외부 API 호출)은 이 트랜잭션 밖에서 EmailVerificationCommandService가 커밋 완료 후 수행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class EmailVerificationWriter {

    private static final long HOURLY_LIMIT = 5;
    private static final long DAILY_LIMIT = 10;
    private static final Duration HOURLY_LOCK_DURATION = Duration.ofHours(1);
    private static final Duration DAILY_LOCK_DURATION = Duration.ofDays(1);

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailVerificationRateLimitRepository rateLimitRepository;

    public String issue(String email, VerificationType type, long codeExpirationMillis) {
        checkRateLimit(type, email);
        invalidatePreviousPendingVerification(email, type);

        String code = VerificationCodeGenerator.generate();
        EmailVerification emailVerification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .type(type)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(codeExpirationMillis)))
                .build();
        emailVerificationRepository.save(emailVerification);

        return code;
    }

    // 인증 한도 초과 여부 확인
    private void checkRateLimit(VerificationType type, String email) {
        if (rateLimitRepository.existsLock(type, email)) {
            log.warn("잠금 상태에서 인증번호 재요청: type={}, email={}", type, EmailMasker.mask(email));
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED);
        }

        long hourlyCount = rateLimitRepository.incrementHourlyCount(type, email);
        if (hourlyCount > HOURLY_LIMIT) {
            log.warn("시간당 인증번호 발송 한도 초과: type={}, email={}, count={}", type, EmailMasker.mask(email), hourlyCount);
            rateLimitRepository.setLock(type, email, HOURLY_LOCK_DURATION);
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED);
        }

        long dailyCount = rateLimitRepository.incrementDailyCount(type, email);
        if (dailyCount > DAILY_LIMIT) {
            log.warn("일일 인증번호 발송 한도 초과: type={}, email={}, count={}", type, EmailMasker.mask(email), dailyCount);
            rateLimitRepository.setLock(type, email, DAILY_LOCK_DURATION);
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED);
        }
    }

    // 새 코드를 발급하면 이전에 보낸 코드는 더 이상 유효하지 않아야 하므로 PENDING 상태를 미리 만료 처리
    private void invalidatePreviousPendingVerification(String email, VerificationType type) {
        emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(email, type, VerificationStatus.PENDING)
                .ifPresent(previous -> {
                    log.debug("기존 PENDING 인증번호 무효화: id={}, type={}, email={}", previous.getId(), type, EmailMasker.mask(email));
                    previous.expire();
                });
    }
}
