package com.cotato.nextstation.domain.auth.service.query;

import com.cotato.nextstation.domain.auth.client.KakaoOAuthClient;
import com.cotato.nextstation.domain.auth.client.dto.KakaoTokenResponse;
import com.cotato.nextstation.domain.auth.client.dto.KakaoUserInfoResponse;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.service.query.result.KakaoLoginResult;
import com.cotato.nextstation.domain.auth.service.query.result.KakaoLoginResultType;
import com.cotato.nextstation.domain.auth.util.KakaoSignupTokenClaims;
import com.cotato.nextstation.domain.auth.util.SignupTokenClaims;
import com.cotato.nextstation.domain.member.entity.AuthProvider;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberSocialAccount;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.member.repository.MemberSocialAccountRepository;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.jwt.AuthTokenClaims;
import com.cotato.nextstation.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KakaoLoginQueryService {

    private static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(14);
    private static final Duration KAKAO_SIGNUP_TOKEN_EXPIRATION = Duration.ofMinutes(10);
    private static final Duration SIGNUP_TOKEN_EXPIRATION = Duration.ofMinutes(30);

    private final KakaoOAuthClient kakaoOAuthClient;
    private final MemberRepository memberRepository;
    private final MemberSocialAccountRepository memberSocialAccountRepository;
    private final JwtProvider jwtProvider;

    // 인가코드로 토큰교환 + 사용자 조회 후 신규/PENDING/기존 회원 3분기 판별, Member 생성은 여기서 하지 않는다(KakaoSignupCommandService 담당)
    public KakaoLoginResult login(String code) {

        KakaoTokenResponse token = kakaoOAuthClient.exchangeToken(code);
        KakaoUserInfoResponse userInfo = kakaoOAuthClient.fetchUserInfo(token.accessToken());
        String providerUserId = userInfo.id().toString();

        Optional<MemberSocialAccount> socialAccount =
                memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, providerUserId);

        if (socialAccount.isEmpty()) {
            log.info("신규 카카오 회원 로그인 시도: providerUserId={}", providerUserId);
            return issueKakaoSignupToken(providerUserId, userInfo);
        }

        Long memberId = socialAccount.get().getMemberId();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    // memberId가 scalar FK라 DB가 정합성을 보장 안 함 -> 정상 흐름에서는 절대 발생하면 안 되는 케이스
                    log.error("member_social_account는 있는데 member가 없음(데이터 정합성 오류): memberId={}", memberId);
                    return new CustomException(AuthErrorCode.MEMBER_NOT_FOUND);
                });

        if (member.getStatus() == MemberStatus.PENDING) {
            log.info("PENDING 상태 카카오 회원 재로그인: memberId={}", member.getId());
            return reissueSignupTokenForPendingMember(member);
        }
        if (member.getStatus() != MemberStatus.ACTIVE) {
            log.warn("ACTIVE/PENDING이 아닌 카카오 회원의 로그인 시도: memberId={}, status={}", member.getId(), member.getStatus());
            throw new CustomException(AuthErrorCode.KAKAO_MEMBER_NOT_ACTIVE);
        }

        log.info("카카오 로그인 성공: memberId={}", member.getId());
        String accessToken = issueAuthToken(member.getId(), AuthTokenClaims.ACCESS_PURPOSE, ACCESS_TOKEN_EXPIRATION);
        String refreshToken = issueAuthToken(member.getId(), AuthTokenClaims.REFRESH_PURPOSE, REFRESH_TOKEN_EXPIRATION);

        return new KakaoLoginResult(KakaoLoginResultType.LOGIN_SUCCESS, member.getId(), accessToken, refreshToken,
                null, null, null, null);
    }

    private KakaoLoginResult issueKakaoSignupToken(String providerUserId, KakaoUserInfoResponse userInfo) {

        // Map.of()는 value가 null이면 NPE를 던지므로, 선택 동의 거부로 null일 수 있는 값들은 빈 문자열로 치환
        Map<String, Object> claims = Map.of(
                KakaoSignupTokenClaims.PURPOSE_KEY, KakaoSignupTokenClaims.KAKAO_SIGNUP_PURPOSE,
                KakaoSignupTokenClaims.EMAIL_KEY, orEmpty(userInfo.extractVerifiedEmail()),
                KakaoSignupTokenClaims.NICKNAME_KEY, orEmpty(userInfo.extractNickname()),
                KakaoSignupTokenClaims.PROFILE_IMAGE_URL_KEY, orEmpty(userInfo.extractProfileImageUrl())
        );

        String kakaoSignupToken = jwtProvider.generateToken(providerUserId, claims, KAKAO_SIGNUP_TOKEN_EXPIRATION);

        return new KakaoLoginResult(KakaoLoginResultType.NEW_MEMBER, null, null, null,
                null, kakaoSignupToken, userInfo.extractNickname(), userInfo.extractProfileImageUrl());
    }

    private KakaoLoginResult reissueSignupTokenForPendingMember(Member member) {

        String signupToken = jwtProvider.generateToken(
                member.getId().toString(),
                Map.of(SignupTokenClaims.PURPOSE_KEY, SignupTokenClaims.SIGNUP_PURPOSE),
                SIGNUP_TOKEN_EXPIRATION
        );
        return new KakaoLoginResult(KakaoLoginResultType.PENDING_PROFILE, member.getId(), null, null,
                signupToken, null, null, null);
    }

    private String issueAuthToken(Long memberId, String purpose, Duration expiration) {
        return jwtProvider.generateToken(memberId.toString(), Map.of(AuthTokenClaims.PURPOSE_KEY, purpose), expiration);
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }
}