package com.cotato.nextstation.domain.auth.service;

import com.cotato.nextstation.domain.auth.repository.RefreshSessionRepository;
import com.cotato.nextstation.global.jwt.AuthTokenClaims;
import com.cotato.nextstation.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

// 로그인 성공 시 access/refresh token을 발급하고 refresh 세션을 Redis에 생성한다.
// 로컬 로그인/카카오 로그인이 이 로직을 공유해야 두 경로 모두 reuse detection 대상이 된다. (한쪽만 빠뜨리면 그 경로는 탈취돼도 탐지 못함)
@Component
@RequiredArgsConstructor
public class AuthTokenIssuer {

    private final JwtProvider jwtProvider;
    private final RefreshSessionRepository refreshSessionRepository;

    public IssuedTokens issue(Long memberId) {
        String accessToken = jwtProvider.generateToken(
                memberId.toString(),
                Map.of(AuthTokenClaims.PURPOSE_KEY, AuthTokenClaims.ACCESS_PURPOSE),
                AuthTokenClaims.ACCESS_TOKEN_EXPIRATION
        );

        String familyId = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtProvider.generateToken(
                memberId.toString(),
                Map.of(
                        AuthTokenClaims.PURPOSE_KEY, AuthTokenClaims.REFRESH_PURPOSE,
                        AuthTokenClaims.FAMILY_ID_KEY, familyId,
                        AuthTokenClaims.JTI_KEY, jti
                ),
                AuthTokenClaims.REFRESH_TOKEN_EXPIRATION
        );

        refreshSessionRepository.create(familyId, memberId, jti);
        return new IssuedTokens(accessToken, refreshToken);
    }
}