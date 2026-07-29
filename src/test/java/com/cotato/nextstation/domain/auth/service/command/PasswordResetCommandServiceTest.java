package com.cotato.nextstation.domain.auth.service.command;

import com.cotato.nextstation.domain.auth.entity.EmailVerification;
import com.cotato.nextstation.domain.auth.entity.VerificationStatus;
import com.cotato.nextstation.domain.auth.entity.VerificationType;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.repository.EmailVerificationRepository;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetCommandServiceTest {

    @InjectMocks
    private PasswordResetCommandService passwordResetCommandService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private static final String EMAIL = "user@example.com";
    private static final String CODE = "123456";
    private static final String NEW_PASSWORD = "newPass12!";

    private EmailVerification verifiedVerification() {
        EmailVerification verification = EmailVerification.builder()
                .email(EMAIL).verificationCode(CODE).type(VerificationType.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(3)).build();
        verification.verify();
        return verification;
    }

    private EmailVerification expiredVerifiedVerification() {
        EmailVerification verification = EmailVerification.builder()
                .email(EMAIL).verificationCode(CODE).type(VerificationType.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().minusMinutes(1)).build();
        verification.verify();
        return verification;
    }

    private Member localMember() {
        Member member = Member.builder().email(EMAIL).password("oldEncoded").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    @Test
    @DisplayName("인증 완료된 상태에서 코드가 일치하면 비밀번호가 변경되고 인증번호는 즉시 만료된다")
    void resetPassword_success() {
        // given
        EmailVerification verification = verifiedVerification();
        Member member = localMember();
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.PASSWORD_RESET, VerificationStatus.VERIFIED))
                .willReturn(Optional.of(verification));
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member));
        given(passwordEncoder.encode(NEW_PASSWORD)).willReturn("newEncoded");

        // when
        passwordResetCommandService.resetPassword(EMAIL, CODE, NEW_PASSWORD, NEW_PASSWORD);

        // then
        assertThat(member.getPassword()).isEqualTo("newEncoded");
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.EXPIRED);
    }

    @Test
    @DisplayName("새 비밀번호와 확인이 다르면 예외가 발생하고 아무것도 변경되지 않는다")
    void resetPassword_passwordConfirmationMismatch() {
        // when & then
        assertThatThrownBy(() -> passwordResetCommandService.resetPassword(EMAIL, CODE, NEW_PASSWORD, "different1!"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH.getMessage());
        verify(emailVerificationRepository, never()).findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                anyString(), any(), any());
    }

    @Test
    @DisplayName("인증 완료(VERIFIED) 내역이 없으면 예외가 발생한다")
    void resetPassword_verificationNotFound() {
        // given
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.PASSWORD_RESET, VerificationStatus.VERIFIED))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> passwordResetCommandService.resetPassword(EMAIL, CODE, NEW_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.EMAIL_VERIFICATION_NOT_FOUND.getMessage());
        verify(memberRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("인증 완료 당시 코드와 요청 코드가 다르면 예외가 발생한다")
    void resetPassword_codeMismatch() {
        // given
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.PASSWORD_RESET, VerificationStatus.VERIFIED))
                .willReturn(Optional.of(verifiedVerification()));

        // when & then
        assertThatThrownBy(() -> passwordResetCommandService.resetPassword(EMAIL, "000000", NEW_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH.getMessage());
        verify(memberRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("확인 이후 시간이 지나 인증번호가 만료됐으면 예외가 발생하고 즉시 만료 처리된다")
    void resetPassword_expired() {
        // given
        EmailVerification verification = expiredVerifiedVerification();
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.PASSWORD_RESET, VerificationStatus.VERIFIED))
                .willReturn(Optional.of(verification));

        // when & then
        assertThatThrownBy(() -> passwordResetCommandService.resetPassword(EMAIL, CODE, NEW_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.EMAIL_VERIFICATION_EXPIRED.getMessage());
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.EXPIRED);
        verify(memberRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("인증은 완료됐지만 회원이 존재하지 않으면 예외가 발생한다")
    void resetPassword_memberNotFound() {
        // given
        given(emailVerificationRepository.findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
                EMAIL, VerificationType.PASSWORD_RESET, VerificationStatus.VERIFIED))
                .willReturn(Optional.of(verifiedVerification()));
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> passwordResetCommandService.resetPassword(EMAIL, CODE, NEW_PASSWORD, NEW_PASSWORD))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.MEMBER_NOT_FOUND.getMessage());
    }
}