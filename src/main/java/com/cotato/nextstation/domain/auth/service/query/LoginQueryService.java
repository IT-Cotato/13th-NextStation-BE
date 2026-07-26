package com.cotato.nextstation.domain.auth.service.query;

import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
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

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginQueryService {

    private static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofHours(1);
    public static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(14);

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

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

        String accessToken = issueToken(member.getId(), AuthTokenClaims.ACCESS_PURPOSE, ACCESS_TOKEN_EXPIRATION);
        String refreshToken = issueToken(member.getId(), AuthTokenClaims.REFRESH_PURPOSE, REFRESH_TOKEN_EXPIRATION);

        log.info("로그인 성공: memberId={}", member.getId());
        return new LoginResult(member.getId(), accessToken, refreshToken);
    }

    // refreshToken 검증 -> accessToken 재발급
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

        String accessToken = issueToken(member.getId(), AuthTokenClaims.ACCESS_PURPOSE, ACCESS_TOKEN_EXPIRATION);
        log.info("accessToken 재발급 성공: memberId={}", member.getId());
        return new ReissueResult(member.getId(), accessToken);
    }

    // 토큰 발급
    private String issueToken(Long memberId, String purpose, Duration expiration) {
        return jwtProvider.generateToken(
                memberId.toString(),
                Map.of(AuthTokenClaims.PURPOSE_KEY, purpose),
                expiration
        );
    }
}