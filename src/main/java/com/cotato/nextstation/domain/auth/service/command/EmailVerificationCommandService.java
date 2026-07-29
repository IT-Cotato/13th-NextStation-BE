package com.cotato.nextstation.domain.auth.service.command;

import com.cotato.nextstation.domain.auth.entity.EmailVerification;
import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import com.cotato.nextstation.domain.auth.entity.VerificationStatus;
import com.cotato.nextstation.domain.auth.entity.VerificationType;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.exception.TermsErrorCode;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRepository;
import com.cotato.nextstation.domain.auth.repository.TermsConsentRepository;
import com.cotato.nextstation.domain.auth.service.EmailVerificationWriter;
import com.cotato.nextstation.domain.auth.util.EmailMasker;
import com.cotato.nextstation.domain.auth.util.VerificationMailSender;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EmailVerificationCommandService {

    private static final int MAX_ATTEMPT_COUNT = 5; // 최대 인증번호 확인 횟수

    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final TermsConsentRepository termsConsentRepository;
    private final EmailVerificationWriter emailVerificationWriter;
    private final VerificationMailSender verificationMailSender;
    private final long codeExpirationMillis;

    public EmailVerificationCommandService(
            MemberRepository memberRepository,
            EmailVerificationRepository emailVerificationRepository,
            TermsConsentRepository termsConsentRepository,
            EmailVerificationWriter emailVerificationWriter,
            VerificationMailSender verificationMailSender,
            @Value("${spring.mail.auth-code-expiration-millis}") long codeExpirationMillis
    ) {
        this.memberRepository = memberRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.termsConsentRepository = termsConsentRepository;
        this.emailVerificationWriter = emailVerificationWriter;
        this.verificationMailSender = verificationMailSender;
        this.codeExpirationMillis = codeExpirationMillis;
    }

    // 회원가입 인증번호 발송
    public void sendSignupVerificationCode(String email, List<Long> agreedTermsIds) {
        log.info("이메일 인증번호 발송 요청: type={}, email={}", VerificationType.SIGNUP, EmailMasker.mask(email));

        validateNotAlreadyRegistered(email);
        validateAgreedTerms(agreedTermsIds);

        // DB/Redis 쓰기는 EmailVerificationWriter의 트랜잭션 안에서 커밋까지 완료된다.
        // 메일 발송(외부 API 호출)은 그 트랜잭션이 끝난 뒤 여기서 별도로 수행되므로
        // SMTP 응답 지연이 DB 커넥션을 점유하지 않는다.
        String code = emailVerificationWriter.issue(email, VerificationType.SIGNUP, codeExpirationMillis);

        verificationMailSender.sendVerificationCode(email, code);
        log.info("이메일 인증번호 발송 완료: type={}, email={}", VerificationType.SIGNUP, EmailMasker.mask(email));
    }

    // 회원가입 인증번호 확인
    @Transactional(noRollbackFor = CustomException.class)
    public void verifySignupCode(String email, String code) {
        verifyCode(VerificationType.SIGNUP, email, code);
    }

    // 비밀번호 재설정 인증번호 발송
    public void sendPasswordResetVerificationCode(String email) {
        log.info("이메일 인증번호 발송 요청: type={}, email={}", VerificationType.PASSWORD_RESET, EmailMasker.mask(email));

        validateResettablePasswordAccount(email);

        // 발송 로직(rate limit/코드 발급/메일 발송)은 회원가입과 동일한 인프라를 재사용한다.
        String code = emailVerificationWriter.issue(email, VerificationType.PASSWORD_RESET, codeExpirationMillis);

        verificationMailSender.sendVerificationCode(email, code);
        log.info("이메일 인증번호 발송 완료: type={}, email={}", VerificationType.PASSWORD_RESET, EmailMasker.mask(email));
    }

    // 비밀번호 재설정 인증번호 확인
    @Transactional(noRollbackFor = CustomException.class)
    public void verifyPasswordResetCode(String email, String code) {
        verifyCode(VerificationType.PASSWORD_RESET, email, code);
    }

    // 발송된 인증번호가 맞는지 확인 (만료/시도횟수/불일치 처리는 인증 목적과 무관하게 동일)
    private void verifyCode(VerificationType type, String email, String code) {
        log.info("이메일 인증번호 확인 요청: type={}, email={}", type, EmailMasker.mask(email));

        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(email, type, VerificationStatus.PENDING)
                .orElseThrow(() -> {
                    log.warn("유효한 인증번호 발송 내역 없음: type={}, email={}", type, EmailMasker.mask(email));
                    return new CustomException(AuthErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
                });

        if (verification.isExpired()) {
            log.warn("만료된 인증번호로 확인 시도: type={}, email={}", type, EmailMasker.mask(email));
            verification.expire();
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }

        if (!verification.getVerificationCode().equals(code)) {
            verification.increaseAttemptCount();
            log.warn("인증번호 불일치: type={}, email={}, attemptCount={}", type, EmailMasker.mask(email), verification.getAttemptCount());

            if (verification.getAttemptCount() >= MAX_ATTEMPT_COUNT) {
                verification.fail();
                log.warn("인증번호 확인 시도 횟수 초과: type={}, email={}", type, EmailMasker.mask(email));
                throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_ATTEMPT_EXCEEDED);
            }
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
        }

        verification.verify();
        log.info("이메일 인증번호 확인 완료: type={}, email={}", type, EmailMasker.mask(email));
    }

    // 비밀번호 재설정이 가능한 계정인지 확인 (가입된 이메일 + 비밀번호 보유한 로컬 계정)
    private void validateResettablePasswordAccount(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 이메일로 비밀번호 재설정 시도: email={}", EmailMasker.mask(email));
                    return new CustomException(AuthErrorCode.MEMBER_NOT_FOUND);
                });

        if (member.getPassword() == null) {
            log.warn("소셜 전용 계정으로 비밀번호 재설정 시도: memberId={}", member.getId());
            throw new CustomException(AuthErrorCode.SOCIAL_ONLY_ACCOUNT);
        }
    }

    // 이미 가입한 이메일인지 확인
    private void validateNotAlreadyRegistered(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }
    }

    // 필수 약관 동의 여부 확인 (비영속 검증)
    private void validateAgreedTerms(List<Long> agreedTermsIds) {
        List<TermsConsent> latestTerms = termsConsentRepository.findAllLatestOrderByRequiredDescIdAsc();

        Set<Long> latestTermsIds = latestTerms.stream()
                .map(TermsConsent::getId)
                .collect(Collectors.toSet());
        if (!latestTermsIds.containsAll(agreedTermsIds)) {
            log.warn("존재하지 않는 약관 id 포함: agreedTermsIds={}", agreedTermsIds);
            throw new CustomException(TermsErrorCode.TERMS_NOT_FOUND);
        }

        Set<Long> requiredTermsIds = latestTerms.stream()
                .filter(TermsConsent::isRequired)
                .map(TermsConsent::getId)
                .collect(Collectors.toSet());
        if (!agreedTermsIds.containsAll(requiredTermsIds)) {
            log.warn("필수 약관 미동의: requiredTermsIds={}, agreedTermsIds={}", requiredTermsIds, agreedTermsIds);
            throw new CustomException(TermsErrorCode.REQUIRED_TERMS_NOT_AGREED);
        }
    }
}