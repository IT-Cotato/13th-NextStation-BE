package com.cotato.nextstation.domain.auth.service.command;

import com.cotato.nextstation.domain.auth.dto.response.SignupResponse;
import com.cotato.nextstation.domain.auth.entity.MemberTermsAgreement;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.repository.MemberTermsAgreementRepository;
import com.cotato.nextstation.domain.auth.util.KakaoSignupTokenClaims;
import com.cotato.nextstation.domain.auth.util.SignupTokenClaims;
import com.cotato.nextstation.domain.auth.util.TermsAgreementValidator;
import com.cotato.nextstation.domain.member.entity.AuthProvider;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberSocialAccount;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.member.repository.MemberSocialAccountRepository;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoSignupCommandService {

    private static final Duration SIGNUP_TOKEN_EXPIRATION = Duration.ofMinutes(30);

    private final MemberRepository memberRepository;
    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final MemberTermsAgreementRepository memberTermsAgreementRepository;
    private final JwtProvider jwtProvider;
    private final TermsAgreementValidator termsAgreementValidator;

    @Transactional
    public SignupResponse signup(String kakaoSignupToken, List<Long> agreedTermsIds, String ipAddress) {

        KakaoSignupClaims kakaoClaims = resolveKakaoClaims(kakaoSignupToken);
        log.info("카카오 회원가입 요청: providerUserId={}", kakaoClaims.providerUserId());

        // 약관 동의 화면이 뜬 사이 중복 요청으로 이미 연동됐으면 재가입이 아니라 signupToken만 재발급
        Optional<MemberSocialAccount> existingSocialAccount = memberSocialAccountRepository
                .findByProviderAndProviderUserId(AuthProvider.KAKAO, kakaoClaims.providerUserId());
        if (existingSocialAccount.isPresent()) {
            return reissueForExistingMember(existingSocialAccount.get().getMemberId());
        }

        termsAgreementValidator.validate(agreedTermsIds);

        String email = kakaoClaims.email().isBlank() ? null : kakaoClaims.email();
        Member member;
        try {
            member = memberRepository.save(Member.builder().email(email).build());
            memberSocialAccountRepository.save(
                    MemberSocialAccount.builder()
                            .memberId(member.getId())
                            .provider(AuthProvider.KAKAO)
                            .providerUserId(kakaoClaims.providerUserId())
                            .email(email)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // 위 조회 이후 동시에 같은 providerUserId로 들어온 요청이 먼저 저장된 경우 (레이스 컨디션)
            // -> Member/MemberSocialAccount 둘 다 같은 트랜잭션이라 여기서 던지면 함께 롤백됨
            log.warn("카카오 회원 중복 저장 시도(레이스 컨디션): providerUserId={}", kakaoClaims.providerUserId());
            throw new CustomException(AuthErrorCode.KAKAO_ACCOUNT_ALREADY_REGISTERED);
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
        log.info("카카오 회원가입 완료: memberId={}", member.getId());
        return new SignupResponse(member.getId(), signupToken);
    }

    private SignupResponse reissueForExistingMember(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.MEMBER_NOT_FOUND));
        if (member.getStatus() != MemberStatus.PENDING) {
            log.warn("이미 프로필 설정까지 완료된 카카오 회원의 재가입 시도: memberId={}", member.getId());
            throw new CustomException(AuthErrorCode.KAKAO_ACCOUNT_ALREADY_REGISTERED);
        }

        String signupToken = issueSignupToken(member.getId());

        log.info("카카오 회원 signupToken 재발급: memberId={}", member.getId());
        return new SignupResponse(member.getId(), signupToken);
    }

    private String issueSignupToken(Long memberId) {
        return jwtProvider.generateToken(
                memberId.toString(),
                Map.of(SignupTokenClaims.PURPOSE_KEY, SignupTokenClaims.SIGNUP_PURPOSE),
                SIGNUP_TOKEN_EXPIRATION
        );
    }

    // subject는 memberId가 아니라 providerUserId(카카오 회원번호)
    private KakaoSignupClaims resolveKakaoClaims(String kakaoSignupToken) {

        Claims claims;

        try {
            claims = jwtProvider.parseClaims(kakaoSignupToken);
        } catch (ExpiredJwtException e) {
            throw new CustomException(AuthErrorCode.KAKAO_SIGNUP_TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new CustomException(AuthErrorCode.INVALID_KAKAO_SIGNUP_TOKEN);
        }

        if (!KakaoSignupTokenClaims.KAKAO_SIGNUP_PURPOSE.equals(claims.get(KakaoSignupTokenClaims.PURPOSE_KEY, String.class))) {
            throw new CustomException(AuthErrorCode.INVALID_KAKAO_SIGNUP_TOKEN);
        }

        String providerUserId = claims.getSubject();
        String email = claims.get(KakaoSignupTokenClaims.EMAIL_KEY, String.class);
        String nickname = claims.get(KakaoSignupTokenClaims.NICKNAME_KEY, String.class);
        String profileImageUrl = claims.get(KakaoSignupTokenClaims.PROFILE_IMAGE_URL_KEY, String.class);
        return new KakaoSignupClaims(providerUserId, email, nickname, profileImageUrl);
    }

    private record KakaoSignupClaims(String providerUserId, String email, String nickname, String profileImageUrl) {
    }
}