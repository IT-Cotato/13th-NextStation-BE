package com.cotato.nextstation.domain.auth.service.query;

import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.member.entity.Gender;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LoginQueryServiceTest {

    @InjectMocks
    private LoginQueryService loginQueryService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

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
        given(jwtProvider.generateToken(eq("1"), any(Map.class), any(Duration.class)))
                .willReturn("access-token", "refresh-token");

        // when
        LoginResult result = loginQueryService.login(EMAIL, PASSWORD);

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
        assertThatThrownBy(() -> loginQueryService.login(EMAIL, PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    @DisplayName("아직 프로필 설정이 끝나지 않은(PENDING) 회원이면 예외가 발생한다")
    void login_memberNotActive() {
        // given
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(pendingMember()));

        // when & then
        assertThatThrownBy(() -> loginQueryService.login(EMAIL, PASSWORD))
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
        assertThatThrownBy(() -> loginQueryService.login(EMAIL, PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_CREDENTIALS.getMessage());
    }
}