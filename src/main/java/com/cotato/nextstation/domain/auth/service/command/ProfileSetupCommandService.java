package com.cotato.nextstation.domain.auth.service.command;

import com.cotato.nextstation.domain.auth.dto.response.ProfileSetupResponse;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.util.SignupTokenClaims;
import com.cotato.nextstation.domain.image.enums.S3Folder;
import com.cotato.nextstation.domain.member.entity.Gender;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import com.cotato.nextstation.domain.member.exception.NicknameErrorCode;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.member.util.NicknameValidator;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;

@Slf4j
@Service
public class ProfileSetupCommandService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final MemberRepository memberRepository;
    private final NicknameValidator nicknameValidator;
    private final JwtProvider jwtProvider;
    private final String expectedProfileImageHost;

    public ProfileSetupCommandService(MemberRepository memberRepository,
                                       NicknameValidator nicknameValidator,
                                       JwtProvider jwtProvider,
                                       @Value("${aws.s3.bucket-name}") String bucketName,
                                       @Value("${spring.cloud.aws.region.static}") String region) {
        this.memberRepository = memberRepository;
        this.nicknameValidator = nicknameValidator;
        this.jwtProvider = jwtProvider;
        this.expectedProfileImageHost = "%s.s3.%s.amazonaws.com".formatted(bucketName, region);
    }

    @Transactional
    public ProfileSetupResponse setupProfile(String authorizationHeader, String nickname, String profileImageUrl,
                                              Gender gender, LocalDate birthDate) {
        Long memberId = resolveMemberId(authorizationHeader);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.MEMBER_NOT_FOUND));

        validateProfileNotAlreadyCompleted(member);
        nicknameValidator.validate(nickname);
        validateProfileImageUrl(profileImageUrl, memberId);

        member.completeProfile(nickname, profileImageUrl, gender, birthDate);
        try {
            memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            // 위 existsByNickname 조회 이후 동시에 같은 닉네임으로 들어온 요청이 먼저 커밋된 경우 (레이스 컨디션)
            log.warn("닉네임 중복 저장 시도(레이스 컨디션): nickname={}", nickname);
            throw new CustomException(NicknameErrorCode.DUPLICATE_NICKNAME);
        }

        return new ProfileSetupResponse(member.getId(), member.getNickname(), member.getStatus());
    }

    // signupToken 파싱/검증 (purpose=SIGNUP인 JWT만 허용) 후 memberId(subject) 추출
    private Long resolveMemberId(String authorizationHeader) {
        String token = extractToken(authorizationHeader);

        Claims claims;
        try {
            claims = jwtProvider.parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw new CustomException(AuthErrorCode.SIGNUP_TOKEN_EXPIRED);
        } catch (JwtException e) {
            throw new CustomException(AuthErrorCode.INVALID_SIGNUP_TOKEN);
        }

        if (!SignupTokenClaims.SIGNUP_PURPOSE.equals(claims.get(SignupTokenClaims.PURPOSE_KEY, String.class))) {
            throw new CustomException(AuthErrorCode.INVALID_SIGNUP_TOKEN);
        }

        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new CustomException(AuthErrorCode.INVALID_SIGNUP_TOKEN);
        }
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new CustomException(AuthErrorCode.INVALID_SIGNUP_TOKEN);
        }
        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }

    // 이미 프로필 설정을 마친 회원의 재요청 차단 (signupToken 재사용 방지 가드)
    private void validateProfileNotAlreadyCompleted(Member member) {
        if (member.getStatus() != MemberStatus.PENDING) {
            throw new CustomException(AuthErrorCode.PROFILE_ALREADY_COMPLETED);
        }
    }

    // profileImageUrl은 선택값이며, 값이 있으면 본인 presigned URL로 발급받은 S3 경로인지 검증한다 (임의 외부 URL/XSS 스킴 차단)
    private void validateProfileImageUrl(String profileImageUrl, Long memberId) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return;
        }

        URI uri;
        try {
            uri = new URI(profileImageUrl);
        } catch (URISyntaxException e) {
            log.warn("파싱할 수 없는 프로필 이미지 URL로 프로필 설정 시도: memberId={}", memberId);
            throw new CustomException(AuthErrorCode.INVALID_PROFILE_IMAGE_URL);
        }

        String expectedPathPrefix = "/%s/%d/".formatted(S3Folder.PROFILE.getPath(), memberId);
        boolean isAllowed = "https".equalsIgnoreCase(uri.getScheme())
                && expectedProfileImageHost.equals(uri.getHost())
                && uri.getRawPath() != null
                && uri.getRawPath().startsWith(expectedPathPrefix);

        if (!isAllowed) {
            log.warn("허용되지 않은 프로필 이미지 URL로 프로필 설정 시도: memberId={}, profileImageUrl={}", memberId, profileImageUrl);
            throw new CustomException(AuthErrorCode.INVALID_PROFILE_IMAGE_URL);
        }
    }
}