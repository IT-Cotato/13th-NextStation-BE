package com.cotato.nextstation.domain.auth.service.command;

import com.cotato.nextstation.domain.auth.dto.response.SignupResponse;
import com.cotato.nextstation.domain.auth.entity.MemberTermsAgreement;
import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import com.cotato.nextstation.domain.auth.entity.VerificationStatus;
import com.cotato.nextstation.domain.auth.entity.VerificationType;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.exception.TermsErrorCode;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRepository;
import com.cotato.nextstation.domain.auth.repository.MemberTermsAgreementRepository;
import com.cotato.nextstation.domain.auth.repository.TermsConsentRepository;
import com.cotato.nextstation.domain.auth.util.EmailMasker;
import com.cotato.nextstation.domain.auth.util.SignupTokenClaims;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupCommandService {

    private static final Duration SIGNUP_TOKEN_EXPIRATION = Duration.ofMinutes(30);

    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final TermsConsentRepository termsConsentRepository;
    private final MemberTermsAgreementRepository memberTermsAgreementRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public SignupResponse signup(String email, String password, String passwordConfirm,
                                  List<Long> agreedTermsIds, String ipAddress) {
        log.info("회원가입 요청: email={}", EmailMasker.mask(email));

        validatePasswordConfirmation(password, passwordConfirm);

        // PENDING 회원은 재가입이 아니라 signupToken 재발급으로 처리 (만료된 토큰 구제)
        Optional<Member> existingMember = memberRepository.findByEmail(email);
        if (existingMember.isPresent()) {
            return reissueForPendingMember(existingMember.get(), password);
        }

        validateEmailVerified(email);
        validateAgreedTerms(agreedTermsIds);

        Member member;
        try {
            member = memberRepository.save(
                    Member.builder()
                            .email(email)
                            .password(passwordEncoder.encode(password))
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // 위 findByEmail 조회 이후 동시에 같은 이메일로 들어온 요청이 먼저 저장된 경우 (레이스 컨디션)
            log.warn("이메일 중복 저장 시도(레이스 컨디션): email={}", EmailMasker.mask(email));
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        List<MemberTermsAgreement> agreements = agreedTermsIds.stream()
                .distinct()
                .map(termsConsentId -> MemberTermsAgreement.builder()
                        .memberId(member.getId())
                        .termsConsentsId(termsConsentId)
                        .agreed(true)
                        .ipAddress(ipAddress)
                        .build())
                .toList();
        memberTermsAgreementRepository.saveAll(agreements);

        String signupToken = issueSignupToken(member.getId());

        log.info("회원가입 완료: memberId={}, email={}", member.getId(), EmailMasker.mask(email));
        return new SignupResponse(member.getId(), signupToken);
    }

    // 비밀번호와 비밀번호 확인 일치 여부 확인
    private void validatePasswordConfirmation(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new CustomException(AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
    }

    // PENDING 회원은 비밀번호로 재인증 후 signupToken만 새로 발급, 그 외 상태(ACTIVE 등)는 이미 가입 완료된 이메일이라 거부
    private SignupResponse reissueForPendingMember(Member member, String password) {
        if (member.getStatus() != MemberStatus.PENDING) {
            log.warn("이미 가입 완료된 이메일로 재요청: memberId={}", member.getId());
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }
        // 비밀번호 재확인 (본인 인증, 없으면 이메일만 알아도 토큰 탈취 가능)
        if (!passwordEncoder.matches(password, member.getPassword())) {
            log.warn("PENDING 회원 signupToken 재발급 시 비밀번호 불일치: memberId={}", member.getId());
            throw new CustomException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        // Member/약관 동의는 최초 가입 시 이미 저장됨
        String signupToken = issueSignupToken(member.getId());
        log.info("signupToken 재발급: memberId={}", member.getId());
        return new SignupResponse(member.getId(), signupToken);
    }

    private String issueSignupToken(Long memberId) {
        return jwtProvider.generateToken(
                memberId.toString(),
                Map.of(SignupTokenClaims.PURPOSE_KEY, SignupTokenClaims.SIGNUP_PURPOSE),
                SIGNUP_TOKEN_EXPIRATION
        );
    }

    // 이메일 인증이 완료된 상태인지 확인
    private void validateEmailVerified(String email) {
        emailVerificationRepository
                .findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(email, VerificationType.SIGNUP, VerificationStatus.VERIFIED)
                .orElseThrow(() -> {
                    log.warn("이메일 인증 미완료 상태에서 회원가입 시도: email={}", EmailMasker.mask(email));
                    return new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
                });
    }

    // 필수 약관 동의 여부 확인
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