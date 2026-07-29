package com.cotato.nextstation.domain.auth.service.command;

import com.cotato.nextstation.domain.auth.entity.EmailVerification;
import com.cotato.nextstation.domain.auth.entity.TermsConsent;
import com.cotato.nextstation.domain.auth.entity.VerificationStatus;
import com.cotato.nextstation.domain.auth.entity.VerificationType;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.exception.TermsErrorCode;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRepository;
import com.cotato.nextstation.domain.auth.repository.TermsConsentRepository;
import com.cotato.nextstation.domain.auth.service.EmailVerificationWriter;
import com.cotato.nextstation.domain.auth.util.VerificationMailSender;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailVerificationCommandServiceTest {

    private EmailVerificationCommandService emailVerificationCommandService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private TermsConsentRepository termsConsentRepository;

    @Mock
    private EmailVerificationWriter emailVerificationWriter;

    @Mock
    private VerificationMailSender verificationMailSender;

    @BeforeEach
    void setUp() {
        emailVerificationCommandService = new EmailVerificationCommandService(
                memberRepository,
                emailVerificationRepository,
                termsConsentRepository,
                emailVerificationWriter,
                verificationMailSender,
                180_000L
        );
    }

    private TermsConsent requiredTerms(Long id) {
        TermsConsent terms = TermsConsent.builder()
                .title("서비스 이용약관").content("내용").version("v1.0").isRequired(true).build();
        ReflectionTestUtils.setField(terms, "id", id);
        return terms;
    }

    private TermsConsent optionalTerms(Long id) {
        TermsConsent terms = TermsConsent.builder()
                .title("마케팅 수신 동의").content("내용").version("v1.0").isRequired(false).build();
        ReflectionTestUtils.setField(terms, "id", id);
        return terms;
    }

    @Test
    @DisplayName("필수 약관에 모두 동의하면 인증번호가 발급되고 메일이 발송된다")
    void sendSignupVerificationCode_success() {
        // given
        String email = "user@example.com";
        given(memberRepository.existsByEmail(email)).willReturn(false);
        given(termsConsentRepository.findAllLatestOrderByRequiredDescIdAsc())
                .willReturn(List.of(requiredTerms(1L), optionalTerms(2L)));
        given(emailVerificationWriter.issue(eq(email), eq(VerificationType.SIGNUP), anyLong())).willReturn("123456");

        // when
        emailVerificationCommandService.sendSignupVerificationCode(email, List.of(1L, 2L));

        // then
        verify(emailVerificationWriter).issue(eq(email), eq(VerificationType.SIGNUP), anyLong());
        verify(verificationMailSender).sendVerificationCode(email, "123456");
    }

    @Test
    @DisplayName("선택 약관을 동의하지 않아도 필수 약관만 동의했으면 발급된다")
    void sendSignupVerificationCode_optionalTermsSkipped() {
        // given
        String email = "user@example.com";
        given(memberRepository.existsByEmail(email)).willReturn(false);
        given(termsConsentRepository.findAllLatestOrderByRequiredDescIdAsc())
                .willReturn(List.of(requiredTerms(1L), optionalTerms(2L)));
        given(emailVerificationWriter.issue(eq(email), eq(VerificationType.SIGNUP), anyLong())).willReturn("123456");

        // when
        emailVerificationCommandService.sendSignupVerificationCode(email, List.of(1L));

        // then
        verify(emailVerificationWriter).issue(eq(email), eq(VerificationType.SIGNUP), anyLong());
    }

    @Test
    @DisplayName("필수 약관을 동의하지 않으면 예외가 발생하고 발급되지 않는다")
    void sendSignupVerificationCode_requiredTermsNotAgreed() {
        // given
        String email = "user@example.com";
        given(memberRepository.existsByEmail(email)).willReturn(false);
        given(termsConsentRepository.findAllLatestOrderByRequiredDescIdAsc())
                .willReturn(List.of(requiredTerms(1L), optionalTerms(2L)));

        // when & then
        assertThatThrownBy(() -> emailVerificationCommandService.sendSignupVerificationCode(email, List.of(2L)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(TermsErrorCode.REQUIRED_TERMS_NOT_AGREED.getMessage());
        verify(emailVerificationWriter, never()).issue(any(), any(), anyLong());
        verify(verificationMailSender, never()).sendVerificationCode(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 약관 id가 포함되면 예외가 발생한다")
    void sendSignupVerificationCode_termsNotFound() {
        // given
        String email = "user@example.com";
        given(memberRepository.existsByEmail(email)).willReturn(false);
        given(termsConsentRepository.findAllLatestOrderByRequiredDescIdAsc())
                .willReturn(List.of(requiredTerms(1L), optionalTerms(2L)));

        // when & then
        assertThatThrownBy(() -> emailVerificationCommandService.sendSignupVerificationCode(email, List.of(1L, 999L)))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(TermsErrorCode.TERMS_NOT_FOUND.getMessage());
        verify(emailVerificationWriter, never()).issue(any(), any(), anyLong());
    }

    private static final String EMAIL = "user@example.com";

    private Member localMember() {
        Member member = Member.builder().email(EMAIL).password("encoded").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    private Member socialOnlyMember() {
        Member member = Member.builder().email(EMAIL).build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    private EmailVerification pendingVerification(VerificationType type) {
        return EmailVerification.builder()
                .email(EMAIL).verificationCode("123456").type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(3)).build();
    }

    private EmailVerification expiredVerification(VerificationType type) {
        return EmailVerification.builder()
                .email(EMAIL).verificationCode("123456").type(type)
                .expiresAt(LocalDateTime.now().minusMinutes(1)).build();
    }

    @Test
    @DisplayName("가입된 로컬 계정이면 비밀번호 재설정 인증번호가 발급되고 메일이 발송된다")
    void sendPasswordResetVerificationCode_success() {
        // given
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(localMember()));
        given(emailVerificationWriter.issue(eq(EMAIL), eq(VerificationType.PASSWORD_RESET), anyLong())).willReturn("123456");

        // when
        emailVerificationCommandService.sendPasswordResetVerificationCode(EMAIL);

        // then
        verify(emailVerificationWriter).issue(eq(EMAIL), eq(VerificationType.PASSWORD_RESET), anyLong());
        verify(verificationMailSender).sendVerificationCode(EMAIL, "123456");
    }

    @Test
    @DisplayName("가입되지 않은 이메일이면 예외가 발생하고 인증번호가 발급되지 않는다")
    void sendPasswordResetVerificationCode_memberNotFound() {
        // given
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> emailVerificationCommandService.sendPasswordResetVerificationCode(EMAIL))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.MEMBER_NOT_FOUND.getMessage());
        verify(emailVerificationWriter, never()).issue(any(), any(), anyLong());
    }

    @Test
    @DisplayName("소셜 로그인 전용 계정이면 예외가 발생하고 인증번호가 발급되지 않는다")
    void sendPasswordResetVerificationCode_socialOnlyAccount() {
        // given
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(socialOnlyMember()));

        // when & then
        assertThatThrownBy(() -> emailVerificationCommandService.sendPasswordResetVerificationCode(EMAIL))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.SOCIAL_ONLY_ACCOUNT.getMessage());
        verify(emailVerificationWriter, never()).issue(any(), any(), anyLong());
    }

    @Test
    @DisplayName("코드가 일치하면 비밀번호 재설정 인증번호가 VERIFIED 상태가 된다")
    void verifyPasswordResetCode_success() {
        // given
        EmailVerification verification = pendingVerification(VerificationType.PASSWORD_RESET);
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.PASSWORD_RESET, VerificationStatus.PENDING))
                .willReturn(Optional.of(verification));

        // when
        emailVerificationCommandService.verifyPasswordResetCode(EMAIL, "123456");

        // then
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    @DisplayName("코드가 일치하지 않으면 예외가 발생하고 시도 횟수가 증가한다")
    void verifyPasswordResetCode_codeMismatch() {
        // given
        EmailVerification verification = pendingVerification(VerificationType.PASSWORD_RESET);
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.PASSWORD_RESET, VerificationStatus.PENDING))
                .willReturn(Optional.of(verification));

        // when & then
        assertThatThrownBy(() -> emailVerificationCommandService.verifyPasswordResetCode(EMAIL, "000000"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH.getMessage());
        assertThat(verification.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("만료된 인증번호로 확인하면 예외가 발생하고 즉시 만료 처리된다")
    void verifyPasswordResetCode_expired() {
        // given
        EmailVerification verification = expiredVerification(VerificationType.PASSWORD_RESET);
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.PASSWORD_RESET, VerificationStatus.PENDING))
                .willReturn(Optional.of(verification));

        // when & then
        assertThatThrownBy(() -> emailVerificationCommandService.verifyPasswordResetCode(EMAIL, "123456"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.EMAIL_VERIFICATION_EXPIRED.getMessage());
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.EXPIRED);
    }

    @Test
    @DisplayName("유효한 발송 내역이 없으면 예외가 발생한다")
    void verifyPasswordResetCode_notFound() {
        // given
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.PASSWORD_RESET, VerificationStatus.PENDING))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> emailVerificationCommandService.verifyPasswordResetCode(EMAIL, "123456"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.EMAIL_VERIFICATION_NOT_FOUND.getMessage());
    }
}
