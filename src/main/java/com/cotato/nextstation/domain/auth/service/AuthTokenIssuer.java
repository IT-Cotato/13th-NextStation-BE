package com.cotato.nextstation.domain.auth.service;

import com.cotato.nextstation.domain.auth.repository.RefreshSessionRepository;
import com.cotato.nextstation.global.jwt.AuthTokenClaims;
import com.cotato.nextstation.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * access/refresh token 발급을 한 곳에 모은다. claim 구성이 여기에만 있어야 claim 추가 시 수정 지점이 하나로 유지된다.
 * 로컬 로그인·카카오 로그인이 {@link #issue}를 공유해야 두 경로 모두 reuse detection 대상이 된다
 * (한쪽만 빠뜨리면 그 경로는 탈취돼도 탐지되지 않음).
 */
@Component
@RequiredArgsConstructor
public class AuthTokenIssuer {

    private final JwtProvider jwtProvider;
    private final RefreshSessionRepository refreshSessionRepository;

    /**
     * 로그인용 — 새 세션(familyId)을 만들어 Redis에 기록하고 토큰을 발급한다.
     */
    public IssuedTokens issue(Long memberId) {
        String familyId = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        // 세션 기록이 실패하면 토큰도 나가지 않도록 발급을 먼저 끝낸다.
        IssuedTokens tokens = reissue(memberId, familyId, jti);
        refreshSessionRepository.create(familyId, memberId, jti);

        // 탈퇴 시 이 회원의 모든 기기 세션을 찾으려면 회원 단위 인덱스가 필요하다.
        refreshSessionRepository.addToMemberIndex(memberId, familyId);
        return tokens;
    }

    /**
     * 재발급용 — 이미 존재하는 세션의 토큰만 다시 발급한다. 세션 상태는 건드리지 않으므로
     * 호출 전에 {@code RefreshSessionRepository.rotate()}로 회전 여부가 판정돼 있어야 한다.
     */
    public IssuedTokens reissue(Long memberId, String familyId, String jti) {
        String accessToken = jwtProvider.generateToken(
                memberId.toString(),
                Map.of(AuthTokenClaims.PURPOSE_KEY, AuthTokenClaims.ACCESS_PURPOSE),
                AuthTokenClaims.ACCESS_TOKEN_EXPIRATION
        );
        String refreshToken = jwtProvider.generateToken(
                memberId.toString(),
                Map.of(
                        AuthTokenClaims.PURPOSE_KEY, AuthTokenClaims.REFRESH_PURPOSE,
                        AuthTokenClaims.FAMILY_ID_KEY, familyId,
                        AuthTokenClaims.JTI_KEY, jti
                ),
                AuthTokenClaims.REFRESH_TOKEN_EXPIRATION
        );
        return new IssuedTokens(accessToken, refreshToken);
    }
}