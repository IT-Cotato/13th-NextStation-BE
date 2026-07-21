package com.cotato.nextstation.domain.auth.service.query;

import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.util.EmailMasker;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.jwt.AuthTokenClaims;
import com.cotato.nextstation.global.jwt.JwtProvider;
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

        return new LoginResult(member.getId(), accessToken, refreshToken);
    }

    private String issueToken(Long memberId, String purpose, Duration expiration) {
        return jwtProvider.generateToken(
                memberId.toString(),
                Map.of(AuthTokenClaims.PURPOSE_KEY, purpose),
                expiration
        );
    }
}