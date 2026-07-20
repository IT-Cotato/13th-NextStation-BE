package com.cotato.nextstation.domain.auth.service.command;

import com.cotato.nextstation.domain.auth.dto.response.ProfileSetupResponse;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.util.SignupTokenClaims;
import com.cotato.nextstation.domain.member.entity.Gender;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import com.cotato.nextstation.domain.member.exception.NicknameErrorCode;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.member.util.NicknameProfanityFilter;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileSetupCommandService {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 10;
    private static final Pattern NICKNAME_ALLOWED_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]+$");

    private final MemberRepository memberRepository;
    private final NicknameProfanityFilter nicknameProfanityFilter;
    private final JwtProvider jwtProvider;

    @Transactional
    public ProfileSetupResponse setupProfile(String authorizationHeader, String nickname, String profileImageUrl,
                                              Gender gender, LocalDate birthDate) {
        Long memberId = resolveMemberId(authorizationHeader);
        log.info("프로필 설정 요청: memberId={}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.MEMBER_NOT_FOUND));

        validateProfileNotAlreadyCompleted(member);
        validateNickname(nickname);

        member.completeProfile(nickname, profileImageUrl, gender, birthDate);
        try {
            memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            // 위 existsByNickname 조회 이후 동시에 같은 닉네임으로 들어온 요청이 먼저 커밋된 경우 (레이스 컨디션)
            log.warn("닉네임 중복 저장 시도(레이스 컨디션): nickname={}", nickname);
            throw new CustomException(NicknameErrorCode.DUPLICATE_NICKNAME);
        }

        log.info("프로필 설정 완료: memberId={}, nickname={}", memberId, nickname);
        return new ProfileSetupResponse(member.getId(), member.getNickname(), member.getStatus());
    }

    // signupToken 파싱/검증 (purpose=SIGNUP인 JWT만 허용) 후 memberId(subject) 추출
    private Long resolveMemberId(String authorizationHeader) {
        String token = extractToken(authorizationHeader);

        Claims claims;
        try {
            claims = jwtProvider.parseClaims(token);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 signupToken으로 프로필 설정 시도");
            throw new CustomException(AuthErrorCode.SIGNUP_TOKEN_EXPIRED);
        } catch (JwtException e) {
            log.warn("유효하지 않은 signupToken으로 프로필 설정 시도: {}", e.getMessage());
            throw new CustomException(AuthErrorCode.INVALID_SIGNUP_TOKEN);
        }

        if (!SignupTokenClaims.SIGNUP_PURPOSE.equals(claims.get(SignupTokenClaims.PURPOSE_KEY, String.class))) {
            log.warn("purpose가 SIGNUP이 아닌 토큰으로 프로필 설정 시도");
            throw new CustomException(AuthErrorCode.INVALID_SIGNUP_TOKEN);
        }

        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            log.warn("subject가 memberId 형식이 아닌 signupToken: subject={}", claims.getSubject());
            throw new CustomException(AuthErrorCode.INVALID_SIGNUP_TOKEN);
        }
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Authorization 헤더 없이 프로필 설정 시도");
            throw new CustomException(AuthErrorCode.INVALID_SIGNUP_TOKEN);
        }
        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }

    // 이미 프로필 설정을 마친 회원의 재요청 차단 (signupToken 재사용 방지 가드)
    private void validateProfileNotAlreadyCompleted(Member member) {
        if (member.getStatus() != MemberStatus.PENDING) {
            log.warn("이미 프로필 설정이 완료된 회원의 재요청: memberId={}, status={}", member.getId(), member.getStatus());
            throw new CustomException(AuthErrorCode.PROFILE_ALREADY_COMPLETED);
        }
    }

    // 길이 -> 허용 문자 -> 금칙어 -> 중복 순으로 검증
    private void validateNickname(String nickname) {
        if (nickname.length() < NICKNAME_MIN_LENGTH) {
            throw new CustomException(NicknameErrorCode.NICKNAME_TOO_SHORT);
        }
        if (nickname.length() > NICKNAME_MAX_LENGTH) {
            throw new CustomException(NicknameErrorCode.NICKNAME_TOO_LONG);
        }
        if (!NICKNAME_ALLOWED_PATTERN.matcher(nickname).matches()) {
            throw new CustomException(NicknameErrorCode.NICKNAME_INVALID_CHARACTER);
        }
        if (nicknameProfanityFilter.containsBannedWord(nickname)) {
            log.warn("금칙어가 포함된 닉네임으로 프로필 설정 시도: nickname={}", nickname);
            throw new CustomException(NicknameErrorCode.NICKNAME_CONTAINS_BANNED_WORD);
        }
        if (memberRepository.existsByNickname(nickname)) {
            log.warn("이미 사용 중인 닉네임으로 프로필 설정 시도: nickname={}", nickname);
            throw new CustomException(NicknameErrorCode.DUPLICATE_NICKNAME);
        }
    }
}