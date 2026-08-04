package com.cotato.nextstation.domain.auth.service.result;

/**
 * AuthTokenService -> AuthController 전달 전용
 * rotation으로 refreshToken도 매번 새로 발급되므로 accessToken과 함께 담아 내려보낸다.
 */
public record ReissueResult(Long memberId, String accessToken, String refreshToken) {
}