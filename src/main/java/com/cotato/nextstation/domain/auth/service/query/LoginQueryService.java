package com.cotato.nextstation.domain.auth.service.query;

import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.repository.RefreshSessionRepository;
import com.cotato.nextstation.domain.auth.service.AuthTokenIssuer;
import com.cotato.nextstation.domain.auth.service.IssuedTokens;
import com.cotato.nextstation.domain.auth.service.query.result.LoginResult;
import com.cotato.nextstation.domain.auth.service.query.result.ReissueResult;
import com.cotato.nextstation.domain.auth.util.EmailMasker;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.jwt.AuthTokenClaims;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginQueryService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthTokenIssuer authTokenIssuer;
    private final RefreshSessionRepository refreshSessionRepository;

    // 로그인
    public LoginResult login(String email, String password) {

        // 이메일 존재 여부와 비밀번호 불일치를 구분하지 않고 동일한 에러로 응답한다 (계정 존재 여부 노출 방지)
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 이메일로 로그인 시도: email={}", EmailMasker.mask(email));
                    return new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
                });

        if (member.getStatus() != MemberStatus.ACTIVE) {
            log.warn("ACTIVE 상태가 아닌 회원의 로그인 시도: memberId={}, status={}", member.getId(), member.getStatus());
            throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(password, member.getPassword())) {
            log.warn("비밀번호 불일치로 로그인 실패: memberId={}", member.getId());
            throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        IssuedTokens tokens = authTokenIssuer.issue(member.getId());

        log.info("로그인 성공: memberId={}", member.getId());
        return new LoginResult(member.getId(), tokens.accessToken(), tokens.refreshToken());
    }

    // refreshToken 검증 -> rotation(reuse detection 포함) -> accessToken/refreshToken 재발급
    public ReissueResult reissue(String refreshToken) {

        Claims claims;
        try {
            claims = jwtProvider.parseClaims(refreshToken);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 refreshToken으로 accessToken 재발급 시도");
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        } catch (JwtException e) {
            log.warn("위변조되었거나 형식이 잘못된 refreshToken으로 accessToken 재발급 시도");
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // purpose로 accessToken/signupToken을 refreshToken 자리에 잘못 흘려넣는 걸 막는다 (AuthTokenClaims 참고)
        if (!AuthTokenClaims.REFRESH_PURPOSE.equals(claims.get(AuthTokenClaims.PURPOSE_KEY, String.class))) {
            log.warn("purpose가 REFRESH가 아닌 토큰으로 accessToken 재발급 시도");
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long memberId;
        try {
            memberId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            log.warn("subject가 memberId 형식이 아닌 refreshToken으로 accessToken 재발급 시도");
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        String familyId = claims.get(AuthTokenClaims.FAMILY_ID_KEY, String.class);
        String jti = claims.get(AuthTokenClaims.JTI_KEY, String.class);
        if (familyId == null || jti == null) {
            // rotation 도입 이전에 발급된 refreshToken - 세션 개념이 없으므로 재로그인을 유도한다.
            log.warn("familyId/jti 클레임이 없는 refreshToken으로 accessToken 재발급 시도: memberId={}", memberId);
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 회원의 refreshToken으로 accessToken 재발급 시도: memberId={}", memberId);
                    return new CustomException(AuthErrorCode.MEMBER_NOT_FOUND);
                });

        // 탈퇴/정지 이후에도 만료 전 refreshToken은 서명 검증만으로는 걸러지지 않으므로, 로그인과 동일하게 DB 상태를 다시 확인한다.
        if (member.getStatus() != MemberStatus.ACTIVE) {
            log.warn("ACTIVE 상태가 아닌 회원의 accessToken 재발급 시도: memberId={}, status={}", member.getId(), member.getStatus());
            throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshSessionRepository.RotateResult rotateResult =
                refreshSessionRepository.rotate(familyId, jti, UUID.randomUUID().toString(), memberId);

        switch (rotateResult.status()) {
            case NOT_FOUND -> {
                log.warn("이미 로그아웃되었거나 만료된 세션의 refreshToken으로 accessToken 재발급 시도: memberId={}, familyId={}", memberId, familyId);
                throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
            }
            case REUSE_DETECTED -> {
                log.error("refreshToken 재사용 탐지(탈취 의심) - 세션 강제 종료: memberId={}, familyId={}", memberId, familyId);
                throw new CustomException(AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
            }
            case MEMBER_MISMATCH -> {
                // 서명이 유효한 토큰의 subject와 세션 소유자가 다른 경우 - 정상 흐름에서는 발생할 수 없다.
                log.error("refreshToken subject와 세션 소유자 불일치 - 세션 강제 종료: memberId={}, familyId={}", memberId, familyId);
                throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
            }
            case GRACE -> log.info("동시 reissue 요청으로 판단해 현재 세션 토큰을 그대로 재사용: memberId={}, familyId={}", memberId, familyId);
            case OK -> log.info("refreshToken rotate 성공: memberId={}, familyId={}", memberId, familyId);
        }

        String accessToken = jwtProvider.generateToken(
                member.getId().toString(),
                Map.of(AuthTokenClaims.PURPOSE_KEY, AuthTokenClaims.ACCESS_PURPOSE),
                AuthTokenClaims.ACCESS_TOKEN_EXPIRATION
        );
        String newRefreshToken = jwtProvider.generateToken(
                member.getId().toString(),
                Map.of(
                        AuthTokenClaims.PURPOSE_KEY, AuthTokenClaims.REFRESH_PURPOSE,
                        AuthTokenClaims.FAMILY_ID_KEY, familyId,
                        AuthTokenClaims.JTI_KEY, rotateResult.jti()
                ),
                AuthTokenClaims.REFRESH_TOKEN_EXPIRATION
        );

        log.info("accessToken 재발급 성공: memberId={}, familyId={}", member.getId(), familyId);
        return new ReissueResult(member.getId(), accessToken, newRefreshToken);
    }

    // 로그아웃 - refreshToken의 familyId로 세션을 삭제한다. 항상 성공(멱등)하며, 실패해도 예외를 던지지 않는다.
    public void logout(String refreshToken) {

        Claims claims;
        try {
            claims = jwtProvider.parseClaims(refreshToken);
        } catch (ExpiredJwtException e) {
            // 서명은 유효했으므로 만료된 토큰이어도 claims를 신뢰해 세션을 정리한다 (로그아웃은 만료 여부를 따지지 않는다).
            claims = e.getClaims();
        } catch (JwtException e) {
            log.warn("위변조되었거나 형식이 잘못된 refreshToken으로 로그아웃 시도");
            return;
        }

        // 다른 용도의 토큰을 쿠키에 넣어 보낸 경우를 막는다 (AuthTokenClaims 참고)
        if (!AuthTokenClaims.REFRESH_PURPOSE.equals(claims.get(AuthTokenClaims.PURPOSE_KEY, String.class))) {
            log.warn("purpose가 REFRESH가 아닌 토큰으로 로그아웃 시도");
            return;
        }

        String familyId = claims.get(AuthTokenClaims.FAMILY_ID_KEY, String.class);
        if (familyId == null) {
            log.warn("familyId 클레임이 없는 refreshToken으로 로그아웃 시도");
            return;
        }

        refreshSessionRepository.delete(familyId);
        log.info("로그아웃 성공: familyId={}", familyId);
    }
}