package com.cotato.nextstation.global.security;

// access token 검증 후 컨트롤러에 주입되는 인증 주체
public record JwtPrincipal(Long memberId) {
}