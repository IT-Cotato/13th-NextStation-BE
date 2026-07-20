package com.cotato.nextstation.domain.auth.service.command;

import com.cotato.nextstation.domain.auth.entity.EmailVerification;
import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import com.cotato.nextstation.domain.auth.entity.VerificationStatus;
import com.cotato.nextstation.domain.auth.entity.VerificationType;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.exception.TermsErrorCode;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRepository;
import com.cotato.nextstation.domain.auth.repository.MemberTermsAgreementRepository;
import com.cotato.nextstation.domain.auth.repository.TermsConsentRepository;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
    private TermsConsentRepository termsConsentRepository;

    @Mock
    private MemberTermsAgreementRepository memberTermsAgreementRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "abc12345!";

    private TermsConsent requiredTerms(Long id) {
        TermsConsent terms = TermsConsent.builder()
                .title("서비스 이용약관").content("내용").version("v1.0").isRequired(true).build();
        ReflectionTestUtils.setField(terms, "id", id);
        return terms;
    }

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

    @Test
    @DisplayName("정상 요청이면 회원이 생성되고 약관 동의가 저장되고 signupToken이 발급된다")
    void signup_success() {
        // given
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.SIGNUP, VerificationStatus.VERIFIED))
                .willReturn(java.util.Optional.of(verifiedEmailVerification()));
        given(termsConsentRepository.findAllLatestOrderByRequiredDescIdAsc())
                .willReturn(List.of(requiredTerms(1L)));
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
        verify(memberRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 예외가 발생한다")
    void signup_duplicateEmail() {
        // given
        given(memberRepository.existsByEmail(EMAIL)).willReturn(true);

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
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.SIGNUP, VerificationStatus.VERIFIED))
                .willReturn(java.util.Optional.empty());

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
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.SIGNUP, VerificationStatus.VERIFIED))
                .willReturn(java.util.Optional.of(verifiedEmailVerification()));
        given(termsConsentRepository.findAllLatestOrderByRequiredDescIdAsc())
                .willReturn(List.of(requiredTerms(1L)));

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
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.SIGNUP, VerificationStatus.VERIFIED))
                .willReturn(java.util.Optional.of(verifiedEmailVerification()));
        given(termsConsentRepository.findAllLatestOrderByRequiredDescIdAsc())
                .willReturn(List.of(requiredTerms(1L)));

        // when & then
        assertThatThrownBy(() -> signupCommandService.signup(EMAIL, PASSWORD, PASSWORD, List.of(1L, 999L), "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(TermsErrorCode.TERMS_NOT_FOUND.getMessage());
        verify(memberRepository, never()).save(any());
    }
}