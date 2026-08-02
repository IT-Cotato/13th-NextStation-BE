package com.cotato.nextstation.domain.auth.service;

/**
 * AuthTokenIssuer -> LoginQueryService/KakaoLoginQueryService 전달 전용
 */
public record IssuedTokens(String accessToken, String refreshToken) {
}