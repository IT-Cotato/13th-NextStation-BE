package com.cotato.nextstation.domain.auth.service.query;

/**
 * LoginQueryService -> AuthController 전달 전용
 */
public record ReissueResult(Long memberId, String accessToken) {
}
