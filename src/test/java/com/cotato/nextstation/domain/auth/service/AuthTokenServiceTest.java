package com.cotato.nextstation.domain.auth.service;

import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.repository.RefreshSessionRepository;
import com.cotato.nextstation.domain.auth.service.result.LoginResult;
import com.cotato.nextstation.domain.auth.service.result.ReissueResult;
import com.cotato.nextstation.domain.member.entity.Gender;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.jwt.AuthTokenClaims;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @InjectMocks
    private AuthTokenService authTokenService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthTokenIssuer authTokenIssuer;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "abc12345!";

    private Member activeMember() {
        Member member = Member.builder().email(EMAIL).password("encoded").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        member.completeProfile("환승러", null, Gender.UNSPECIFIED, LocalDate.of(2000, 1, 1));
        return member;
    }

    private Member pendingMember() {
        Member member = Member.builder().email(EMAIL).password("encoded").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    @Test
    @DisplayName("이메일/비밀번호가 맞는 ACTIVE 회원이면 access token과 refresh token을 발급한다")
    void login_success() {
        // given
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(activeMember()));
        given(passwordEncoder.matches(PASSWORD, "encoded")).willReturn(true);
        given(authTokenIssuer.issue(1L)).willReturn(new IssuedTokens("access-token", "refresh-token"));

        // when
        LoginResult result = authTokenService.login(EMAIL, PASSWORD);

        // then
        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 예외가 발생한다")
    void login_emailNotFound() {
        // given
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authTokenService.login(EMAIL, PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    @DisplayName("아직 프로필 설정이 끝나지 않은(PENDING) 회원이면 예외가 발생한다")
    void login_memberNotActive() {
        // given
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(pendingMember()));

        // when & then
        assertThatThrownBy(() -> authTokenService.login(EMAIL, PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 예외가 발생한다")
    void login_passwordMismatch() {
        // given
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(activeMember()));
        given(passwordEncoder.matches(PASSWORD, "encoded")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authTokenService.login(EMAIL, PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_CREDENTIALS.getMessage());
    }

    private Claims refreshClaimsWithFamily(String subject, String familyId, String jti) {
        Claims claims = mock(Claims.class);
        given(claims.get(AuthTokenClaims.PURPOSE_KEY, String.class)).willReturn(AuthTokenClaims.REFRESH_PURPOSE);
        given(claims.getSubject()).willReturn(subject);
        given(claims.get(AuthTokenClaims.FAMILY_ID_KEY, String.class)).willReturn(familyId);
        given(claims.get(AuthTokenClaims.JTI_KEY, String.class)).willReturn(jti);
        return claims;
    }

    @Test
    @DisplayName("유효한 refreshToken이고 ACTIVE 회원이면 rotate하여 새 accessToken/refreshToken을 발급한다")
    void reissue_success() {
        // given
        Claims claims = refreshClaimsWithFamily("1", "family-1", "jti-1");
        given(jwtProvider.parseClaims("refresh-token")).willReturn(claims);
        given(memberRepository.findById(1L)).willReturn(Optional.of(activeMember()));
        given(refreshSessionRepository.rotate(eq("family-1"), eq("jti-1"), anyString(), eq(1L)))
                .willReturn(new RefreshSessionRepository.RotateResult(RefreshSessionRepository.RotateStatus.OK, "jti-2"));
        given(authTokenIssuer.reissue(1L, "family-1", "jti-2"))
                .willReturn(new IssuedTokens("new-access-token", "new-refresh-token"));

        // when
        ReissueResult result = authTokenService.reissue("refresh-token");

        // then
        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("rotate 직후 grace 안에 같은 토큰으로 다시 오면(동시 요청) 오탐 없이 현재 세션 토큰으로 재발급한다")
    void reissue_graceWindow() {
        // given
        Claims claims = refreshClaimsWithFamily("1", "family-1", "jti-1");
        given(jwtProvider.parseClaims("refresh-token")).willReturn(claims);
        given(memberRepository.findById(1L)).willReturn(Optional.of(activeMember()));
        given(refreshSessionRepository.rotate(eq("family-1"), eq("jti-1"), anyString(), eq(1L)))
                .willReturn(new RefreshSessionRepository.RotateResult(RefreshSessionRepository.RotateStatus.GRACE, "jti-2"));
        // grace에서는 rotate하지 않으므로 세션의 현재 jti(jti-2)로 발급되어야 한다
        given(authTokenIssuer.reissue(1L, "family-1", "jti-2"))
                .willReturn(new IssuedTokens("new-access-token", "current-refresh-token"));

        // when
        ReissueResult result = authTokenService.reissue("refresh-token");

        // then
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("current-refresh-token");
    }

    @Test
    @DisplayName("세션 소유자와 토큰 subject가 다르면 예외가 발생한다")
    void reissue_memberMismatch() {
        // given
        Claims claims = refreshClaimsWithFamily("1", "family-1", "jti-1");
        given(jwtProvider.parseClaims("refresh-token")).willReturn(claims);
        given(memberRepository.findById(1L)).willReturn(Optional.of(activeMember()));
        given(refreshSessionRepository.rotate(eq("family-1"), eq("jti-1"), anyString(), eq(1L)))
                .willReturn(new RefreshSessionRepository.RotateResult(RefreshSessionRepository.RotateStatus.MEMBER_MISMATCH, null));

        // when & then
        assertThatThrownBy(() -> authTokenService.reissue("refresh-token"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    @Test
    @DisplayName("familyId/jti 클레임이 없는(rotation 도입 이전) refreshToken이면 예외가 발생한다")
    void reissue_legacyTokenWithoutFamilyId() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get(AuthTokenClaims.PURPOSE_KEY, String.class)).willReturn(AuthTokenClaims.REFRESH_PURPOSE);
        given(claims.getSubject()).willReturn("1");
        given(jwtProvider.parseClaims("refresh-token")).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> authTokenService.reissue("refresh-token"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    @Test
    @DisplayName("이미 로그아웃되었거나 만료된 세션이면 예외가 발생한다")
    void reissue_sessionNotFound() {
        // given
        Claims claims = refreshClaimsWithFamily("1", "family-1", "jti-1");
        given(jwtProvider.parseClaims("refresh-token")).willReturn(claims);
        given(memberRepository.findById(1L)).willReturn(Optional.of(activeMember()));
        given(refreshSessionRepository.rotate(eq("family-1"), eq("jti-1"), anyString(), eq(1L)))
                .willReturn(new RefreshSessionRepository.RotateResult(RefreshSessionRepository.RotateStatus.NOT_FOUND, null));

        // when & then
        assertThatThrownBy(() -> authTokenService.reissue("refresh-token"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    @Test
    @DisplayName("이미 rotate되어 무효화된 옛 jti가 재사용되면 재사용 탐지 예외가 발생하고 세션이 종료된다")
    void reissue_reuseDetected() {
        // given
        Claims claims = refreshClaimsWithFamily("1", "family-1", "old-jti");
        given(jwtProvider.parseClaims("refresh-token")).willReturn(claims);
        given(memberRepository.findById(1L)).willReturn(Optional.of(activeMember()));
        given(refreshSessionRepository.rotate(eq("family-1"), eq("old-jti"), anyString(), eq(1L)))
                .willReturn(new RefreshSessionRepository.RotateResult(RefreshSessionRepository.RotateStatus.REUSE_DETECTED, null));

        // when & then
        assertThatThrownBy(() -> authTokenService.reissue("refresh-token"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED.getMessage());
    }

    @Test
    @DisplayName("만료된 refreshToken이면 예외가 발생한다")
    void reissue_expiredToken() {
        // given
        given(jwtProvider.parseClaims("refresh-token"))
                .willThrow(new ExpiredJwtException(null, null, "expired"));

        // when & then
        assertThatThrownBy(() -> authTokenService.reissue("refresh-token"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.REFRESH_TOKEN_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("위변조된 refreshToken이면 예외가 발생한다")
    void reissue_invalidToken() {
        // given
        given(jwtProvider.parseClaims("refresh-token"))
                .willThrow(new JwtException("invalid signature"));

        // when & then
        assertThatThrownBy(() -> authTokenService.reissue("refresh-token"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    @Test
    @DisplayName("purpose가 REFRESH가 아니면 예외가 발생한다 (accessToken을 refreshToken 자리에 넣은 경우)")
    void reissue_purposeMismatch() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get(AuthTokenClaims.PURPOSE_KEY, String.class)).willReturn(AuthTokenClaims.ACCESS_PURPOSE);
        given(jwtProvider.parseClaims("access-token")).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> authTokenService.reissue("access-token"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    @Test
    @DisplayName("subject가 숫자가 아니면 예외가 발생한다")
    void reissue_invalidSubject() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get(AuthTokenClaims.PURPOSE_KEY, String.class)).willReturn(AuthTokenClaims.REFRESH_PURPOSE);
        given(claims.getSubject()).willReturn("not-a-number");
        given(jwtProvider.parseClaims("refresh-token")).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> authTokenService.reissue("refresh-token"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 회원의 refreshToken이면 예외가 발생한다")
    void reissue_memberNotFound() {
        // given
        Claims claims = refreshClaimsWithFamily("999", "family-1", "jti-1");
        given(jwtProvider.parseClaims("refresh-token")).willReturn(claims);
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authTokenService.reissue("refresh-token"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("ACTIVE 상태가 아닌 회원이면 예외가 발생한다")
    void reissue_memberNotActive() {
        // given
        Claims claims = refreshClaimsWithFamily("1", "family-1", "jti-1");
        given(jwtProvider.parseClaims("refresh-token")).willReturn(claims);
        given(memberRepository.findById(1L)).willReturn(Optional.of(pendingMember()));

        // when & then
        assertThatThrownBy(() -> authTokenService.reissue("refresh-token"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_REFRESH_TOKEN.getMessage());
    }

    @Test
    @DisplayName("정상 familyId가 있으면 세션을 삭제하고 로그아웃한다")
    void logout_success() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get(AuthTokenClaims.PURPOSE_KEY, String.class)).willReturn(AuthTokenClaims.REFRESH_PURPOSE);
        given(claims.get(AuthTokenClaims.FAMILY_ID_KEY, String.class)).willReturn("family-1");
        given(jwtProvider.parseClaims("refresh-token")).willReturn(claims);

        // when
        authTokenService.logout("refresh-token");

        // then
        org.mockito.Mockito.verify(refreshSessionRepository).delete("family-1");
    }

    @Test
    @DisplayName("만료된 refreshToken이어도 familyId를 추출해 세션을 삭제한다")
    void logout_expiredTokenStillInvalidatesSession() {
        // given
        Claims expiredClaims = mock(Claims.class);
        given(expiredClaims.get(AuthTokenClaims.PURPOSE_KEY, String.class)).willReturn(AuthTokenClaims.REFRESH_PURPOSE);
        given(expiredClaims.get(AuthTokenClaims.FAMILY_ID_KEY, String.class)).willReturn("family-1");
        ExpiredJwtException expiredJwtException = mock(ExpiredJwtException.class);
        given(expiredJwtException.getClaims()).willReturn(expiredClaims);
        given(jwtProvider.parseClaims("refresh-token")).willThrow(expiredJwtException);

        // when
        authTokenService.logout("refresh-token");

        // then
        org.mockito.Mockito.verify(refreshSessionRepository).delete("family-1");
    }

    @Test
    @DisplayName("purpose가 REFRESH가 아닌 토큰으로 로그아웃하면 세션을 건드리지 않는다")
    void logout_purposeMismatchNoop() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get(AuthTokenClaims.PURPOSE_KEY, String.class)).willReturn(AuthTokenClaims.ACCESS_PURPOSE);
        given(jwtProvider.parseClaims("access-token")).willReturn(claims);

        // when & then
        authTokenService.logout("access-token");
        org.mockito.Mockito.verify(refreshSessionRepository, org.mockito.Mockito.never()).delete(anyString());
    }

    @Test
    @DisplayName("위변조된 refreshToken으로 로그아웃해도 예외 없이 조용히 종료한다")
    void logout_invalidTokenNoop() {
        // given
        given(jwtProvider.parseClaims("bad-token")).willThrow(new JwtException("invalid signature"));

        // when & then (예외를 던지지 않아야 한다)
        authTokenService.logout("bad-token");
        org.mockito.Mockito.verify(refreshSessionRepository, org.mockito.Mockito.never()).delete(anyString());
    }

    @Test
    @DisplayName("familyId 클레임이 없는(rotation 도입 이전) refreshToken으로 로그아웃해도 예외 없이 조용히 종료한다")
    void logout_missingFamilyIdNoop() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get(AuthTokenClaims.PURPOSE_KEY, String.class)).willReturn(AuthTokenClaims.REFRESH_PURPOSE);
        given(claims.get(AuthTokenClaims.FAMILY_ID_KEY, String.class)).willReturn(null);
        given(jwtProvider.parseClaims("legacy-refresh-token")).willReturn(claims);

        // when & then
        authTokenService.logout("legacy-refresh-token");
        org.mockito.Mockito.verify(refreshSessionRepository, org.mockito.Mockito.never()).delete(anyString());
    }
}