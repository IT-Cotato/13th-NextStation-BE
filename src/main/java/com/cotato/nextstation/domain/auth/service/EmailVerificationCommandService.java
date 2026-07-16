package com.cotato.nextstation.domain.auth.service;

import com.cotato.nextstation.domain.auth.entity.EmailVerification;
import com.cotato.nextstation.domain.auth.entity.VerificationStatus;
import com.cotato.nextstation.domain.auth.entity.VerificationType;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRateLimitRepository;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRepository;
import com.cotato.nextstation.domain.auth.util.VerificationCodeGenerator;
import com.cotato.nextstation.domain.auth.util.VerificationMailSender;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class EmailVerificationCommandService {

    private static final long HOURLY_LIMIT = 5;
    private static final long DAILY_LIMIT = 10;
    private static final Duration HOURLY_LOCK_DURATION = Duration.ofHours(1);
    private static final Duration DAILY_LOCK_DURATION = Duration.ofDays(1);

    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailVerificationRateLimitRepository rateLimitRepository;
    private final VerificationMailSender verificationMailSender;
    private final long codeExpirationMillis;

    public EmailVerificationCommandService(
            MemberRepository memberRepository,
            EmailVerificationRepository emailVerificationRepository,
            EmailVerificationRateLimitRepository rateLimitRepository,
            VerificationMailSender verificationMailSender,
            @Value("${spring.mail.auth-code-expiration-millis}") long codeExpirationMillis
    ) {
        this.memberRepository = memberRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.rateLimitRepository = rateLimitRepository;
        this.verificationMailSender = verificationMailSender;
        this.codeExpirationMillis = codeExpirationMillis;
    }

    // 회원가입 인증
    public void sendSignupVerificationCode(String email) {
        log.info("이메일 인증코드 발송 요청: type={}, email={}", VerificationType.SIGNUP, email);

        validateNotAlreadyRegistered(email);
        checkRateLimit(VerificationType.SIGNUP, email);
        invalidatePreviousPendingVerification(email, VerificationType.SIGNUP);

        String code = VerificationCodeGenerator.generate();
        EmailVerification emailVerification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .type(VerificationType.SIGNUP)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(codeExpirationMillis)))
                .build();
        emailVerificationRepository.save(emailVerification);

        verificationMailSender.sendVerificationCode(email, code);
        log.info("이메일 인증코드 발송 완료: type={}, email={}", VerificationType.SIGNUP, email);
    }

    // 이미 가입한 이메일인지 확인
    private void validateNotAlreadyRegistered(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }
    }

    // 인증 한도 초과 여부 확인
    private void checkRateLimit(VerificationType type, String email) {
        if (rateLimitRepository.existsLock(type, email)) {
            log.warn("잠금 상태에서 인증코드 재요청: type={}, email={}", type, email);
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED);
        }

        long hourlyCount = rateLimitRepository.incrementHourlyCount(type, email);
        if (hourlyCount > HOURLY_LIMIT) {
            log.warn("시간당 인증코드 발송 한도 초과: type={}, email={}, count={}", type, email, hourlyCount);
            rateLimitRepository.setLock(type, email, HOURLY_LOCK_DURATION);
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED);
        }

        long dailyCount = rateLimitRepository.incrementDailyCount(type, email);
        if (dailyCount > DAILY_LIMIT) {
            log.warn("일일 인증코드 발송 한도 초과: type={}, email={}, count={}", type, email, dailyCount);
            rateLimitRepository.setLock(type, email, DAILY_LOCK_DURATION);
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED);
        }
    }

    // 새 코드를 발급하면 이전에 보낸 코드는 더 이상 유효하지 않아야 하므로 PENDING 상태를 미리 만료 처리
    private void invalidatePreviousPendingVerification(String email, VerificationType type) {
        emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(email, type, VerificationStatus.PENDING)
                .ifPresent(previous -> {
                    log.debug("기존 PENDING 인증코드 무효화: id={}, type={}, email={}", previous.getId(), type, email);
                    previous.expire();
                });
    }
}