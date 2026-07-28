package com.cotato.nextstation.domain.auth.service.command;

import com.cotato.nextstation.domain.auth.entity.EmailVerification;
import com.cotato.nextstation.domain.auth.entity.VerificationStatus;
import com.cotato.nextstation.domain.auth.entity.VerificationType;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.exception.TermsErrorCode;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRepository;
import com.cotato.nextstation.domain.auth.repository.MemberTermsAgreementRepository;
import com.cotato.nextstation.domain.auth.util.TermsAgreementValidator;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SignupCommandServiceTest {

    @InjectMocks
    private SignupCommandService signupCommandService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private MemberTermsAgreementRepository memberTermsAgreementRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TermsAgreementValidator termsAgreementValidator;

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "abc12345!";

    private EmailVerification verifiedEmailVerification() {
        EmailVerification verification = EmailVerification.builder()
                .email(EMAIL).verificationCode("123456").type(VerificationType.SIGNUP)
                .expiresAt(java.time.LocalDateTime.now().plusMinutes(3)).build();
        verification.verify();
        return verification;
    }

    private Member savedMember() {
        Member member = Member.builder().email(EMAIL).password("encoded").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    private Member pendingMember() {
        Member member = Member.builder().email(EMAIL).password("encoded").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    private Member activeMember() {
        Member member = Member.builder().email(EMAIL).password("encoded").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        member.completeProfile("기존닉네임", null, Gender.UNSPECIFIED, LocalDate.of(2000, 1, 1));
        return member;
    }

    @Test
    @DisplayName("정상 요청이면 회원이 생성되고 약관 동의가 저장되고 signupToken이 발급된다")
    void signup_success() {
        // given
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.SIGNUP, VerificationStatus.VERIFIED))
                .willReturn(Optional.of(verifiedEmailVerification()));
        given(passwordEncoder.encode(PASSWORD)).willReturn("encoded");
        given(memberRepository.save(any(Member.class))).willReturn(savedMember());
        given(jwtProvider.generateToken(eq("1"), any(Map.class), any(Duration.class))).willReturn("signup-token");

        // when
        var response = signupCommandService.signup(EMAIL, PASSWORD, PASSWORD, List.of(1L), "127.0.0.1");

        // then
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.signupToken()).isEqualTo("signup-token");
        verify(memberTermsAgreementRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("비밀번호와 비밀번호 확인이 다르면 예외가 발생하고 아무것도 저장되지 않는다")
    void signup_passwordConfirmationMismatch() {
        // when & then
        assertThatThrownBy(() -> signupCommandService.signup(EMAIL, PASSWORD, "different1!", List.of(1L), "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH.getMessage());
        verify(memberRepository, never()).save(any());
        verify(memberRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("이미 프로필 설정까지 완료된(ACTIVE) 이메일이면 예외가 발생한다")
    void signup_duplicateEmail() {
        // given
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(activeMember()));

        // when & then
        assertThatThrownBy(() -> signupCommandService.signup(EMAIL, PASSWORD, PASSWORD, List.of(1L), "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.DUPLICATE_EMAIL.getMessage());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("이메일 인증이 완료되지 않았으면 예외가 발생한다")
    void signup_emailNotVerified() {
        // given
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.SIGNUP, VerificationStatus.VERIFIED))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> signupCommandService.signup(EMAIL, PASSWORD, PASSWORD, List.of(1L), "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.EMAIL_NOT_VERIFIED.getMessage());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("필수 약관을 동의하지 않으면 예외가 발생한다")
    void signup_requiredTermsNotAgreed() {
        // given
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.SIGNUP, VerificationStatus.VERIFIED))
                .willReturn(Optional.of(verifiedEmailVerification()));
        willThrow(new CustomException(TermsErrorCode.REQUIRED_TERMS_NOT_AGREED))
                .given(termsAgreementValidator).validate(List.of());

        // when & then
        assertThatThrownBy(() -> signupCommandService.signup(EMAIL, PASSWORD, PASSWORD, List.of(), "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(TermsErrorCode.REQUIRED_TERMS_NOT_AGREED.getMessage());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 약관 id가 포함되면 예외가 발생한다")
    void signup_termsNotFound() {
        // given
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.SIGNUP, VerificationStatus.VERIFIED))
                .willReturn(Optional.of(verifiedEmailVerification()));
        willThrow(new CustomException(TermsErrorCode.TERMS_NOT_FOUND))
                .given(termsAgreementValidator).validate(List.of(1L, 999L));

        // when & then
        assertThatThrownBy(() -> signupCommandService.signup(EMAIL, PASSWORD, PASSWORD, List.of(1L, 999L), "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(TermsErrorCode.TERMS_NOT_FOUND.getMessage());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("signupToken 만료 후 돌아온 PENDING 회원이 비밀번호를 맞게 입력하면 새 회원을 만들지 않고 signupToken만 재발급한다")
    void signup_reissueForPendingMember() {
        // given
        Member member = pendingMember();
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(PASSWORD, "encoded")).willReturn(true);
        given(jwtProvider.generateToken(eq("1"), any(Map.class), any(Duration.class))).willReturn("reissued-token");

        // when
        var response = signupCommandService.signup(EMAIL, PASSWORD, PASSWORD, List.of(1L), "127.0.0.1");

        // then
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.signupToken()).isEqualTo("reissued-token");
        verify(memberRepository, never()).save(any());
        verify(memberTermsAgreementRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("PENDING 회원인데 비밀번호가 다르면 예외가 발생한다")
    void signup_reissuePasswordMismatch() {
        // given
        Member member = pendingMember();
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(PASSWORD, "encoded")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> signupCommandService.signup(EMAIL, PASSWORD, PASSWORD, List.of(1L), "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.PASSWORD_MISMATCH.getMessage());
        verify(memberRepository, never()).save(any());
    }
}