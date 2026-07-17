package com.cotato.nextstation.domain.auth.service.command;

import com.cotato.nextstation.domain.auth.entity.EmailVerification;
import com.cotato.nextstation.domain.auth.entity.VerificationStatus;
import com.cotato.nextstation.domain.auth.entity.VerificationType;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRepository;
import com.cotato.nextstation.domain.auth.service.EmailVerificationWriter;
import com.cotato.nextstation.domain.auth.util.EmailMasker;
import com.cotato.nextstation.domain.auth.util.VerificationMailSender;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class EmailVerificationCommandService {

    private static final int MAX_ATTEMPT_COUNT = 5; // 최대 인증번호 확인 횟수

    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailVerificationWriter emailVerificationWriter;
    private final VerificationMailSender verificationMailSender;
    private final long codeExpirationMillis;

    public EmailVerificationCommandService(
            MemberRepository memberRepository,
            EmailVerificationRepository emailVerificationRepository,
            EmailVerificationWriter emailVerificationWriter,
            VerificationMailSender verificationMailSender,
            @Value("${spring.mail.auth-code-expiration-millis}") long codeExpirationMillis
    ) {
        this.memberRepository = memberRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.emailVerificationWriter = emailVerificationWriter;
        this.verificationMailSender = verificationMailSender;
        this.codeExpirationMillis = codeExpirationMillis;
    }

    // 회원가입 인증번호 발송
    public void sendSignupVerificationCode(String email) {
        log.info("이메일 인증번호 발송 요청: type={}, email={}", VerificationType.SIGNUP, EmailMasker.mask(email));

        validateNotAlreadyRegistered(email);

        // DB/Redis 쓰기는 EmailVerificationWriter의 트랜잭션 안에서 커밋까지 완료된다.
        // 메일 발송(외부 API 호출)은 그 트랜잭션이 끝난 뒤 여기서 별도로 수행되므로
        // SMTP 응답 지연이 DB 커넥션을 점유하지 않는다.
        String code = emailVerificationWriter.issue(email, VerificationType.SIGNUP, codeExpirationMillis);

        verificationMailSender.sendVerificationCode(email, code);
        log.info("이메일 인증번호 발송 완료: type={}, email={}", VerificationType.SIGNUP, EmailMasker.mask(email));
    }

    // 회원가입 인증번호 확인
    @Transactional
    public void verifySignupCode(String email, String code) {
        log.info("이메일 인증번호 확인 요청: type={}, email={}", VerificationType.SIGNUP, EmailMasker.mask(email));

        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(email, VerificationType.SIGNUP, VerificationStatus.PENDING)
                .orElseThrow(() -> {
                    log.warn("유효한 인증번호 발송 내역 없음: type={}, email={}", VerificationType.SIGNUP, EmailMasker.mask(email));
                    return new CustomException(AuthErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
                });

        if (verification.isExpired()) {
            log.warn("만료된 인증번호로 확인 시도: type={}, email={}", VerificationType.SIGNUP, EmailMasker.mask(email));
            verification.expire();
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }

        if (!verification.getVerificationCode().equals(code)) {
            verification.increaseAttemptCount();
            log.warn("인증번호 불일치: type={}, email={}, attemptCount={}", VerificationType.SIGNUP, EmailMasker.mask(email), verification.getAttemptCount());

            if (verification.getAttemptCount() >= MAX_ATTEMPT_COUNT) {
                verification.fail();
                log.warn("인증번호 확인 시도 횟수 초과: type={}, email={}", VerificationType.SIGNUP, EmailMasker.mask(email));
                throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_ATTEMPT_EXCEEDED);
            }
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
        }

        verification.verify();
        log.info("이메일 인증번호 확인 완료: type={}, email={}", VerificationType.SIGNUP, EmailMasker.mask(email));
    }

    // 이미 가입한 이메일인지 확인
    private void validateNotAlreadyRegistered(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }
    }
}