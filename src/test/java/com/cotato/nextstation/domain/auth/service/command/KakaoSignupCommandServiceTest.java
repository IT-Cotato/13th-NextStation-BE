package com.cotato.nextstation.domain.auth.service.command;

import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.exception.TermsErrorCode;
import com.cotato.nextstation.domain.auth.repository.MemberTermsAgreementRepository;
import com.cotato.nextstation.domain.auth.util.KakaoSignupTokenClaims;
import com.cotato.nextstation.domain.auth.util.TermsAgreementValidator;
import com.cotato.nextstation.domain.member.entity.AuthProvider;
import com.cotato.nextstation.domain.member.entity.Gender;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberSocialAccount;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.member.repository.MemberSocialAccountRepository;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KakaoSignupCommandServiceTest {

    @InjectMocks
    private KakaoSignupCommandService kakaoSignupCommandService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberSocialAccountRepository memberSocialAccountRepository;

    @Mock
    private MemberTermsAgreementRepository memberTermsAgreementRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TermsAgreementValidator termsAgreementValidator;

    private static final String KAKAO_SIGNUP_TOKEN = "kakao-signup-token";
    private static final String PROVIDER_USER_ID = "555";

    private Claims validClaims(String email, String nickname, String profileImageUrl) {
        Claims claims = mock(Claims.class);
        given(claims.get(KakaoSignupTokenClaims.PURPOSE_KEY, String.class)).willReturn(KakaoSignupTokenClaims.KAKAO_SIGNUP_PURPOSE);
        given(claims.getSubject()).willReturn(PROVIDER_USER_ID);
        given(claims.get(KakaoSignupTokenClaims.EMAIL_KEY, String.class)).willReturn(email);
        given(claims.get(KakaoSignupTokenClaims.NICKNAME_KEY, String.class)).willReturn(nickname);
        given(claims.get(KakaoSignupTokenClaims.PROFILE_IMAGE_URL_KEY, String.class)).willReturn(profileImageUrl);
        return claims;
    }

    private Member savedMember() {
        Member member = Member.builder().email(null).build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    private Member pendingMember() {
        Member member = Member.builder().email(null).build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    private Member activeMember() {
        Member member = Member.builder().email(null).build();
        ReflectionTestUtils.setField(member, "id", 1L);
        member.completeProfile("기존닉네임", null, Gender.UNSPECIFIED, LocalDate.of(2000, 1, 1));
        return member;
    }

    private MemberSocialAccount socialAccount(Long memberId) {
        return MemberSocialAccount.builder()
                .memberId(memberId)
                .provider(AuthProvider.KAKAO)
                .providerUserId(PROVIDER_USER_ID)
                .email(null)
                .build();
    }

    @Test
    @DisplayName("정상 요청이면 Member와 MemberSocialAccount가 생성되고 약관 동의가 저장되고 signupToken이 발급된다")
    void signup_success() {
        // given
        Claims claims = validClaims("user@kakao.com", "환승러", "http://img");
        given(jwtProvider.parseClaims(KAKAO_SIGNUP_TOKEN)).willReturn(claims);
        given(memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, PROVIDER_USER_ID))
                .willReturn(Optional.empty());
        given(memberRepository.save(any(Member.class))).willReturn(savedMember());
        given(jwtProvider.generateToken(eq("1"), any(Map.class), any(Duration.class))).willReturn("signup-token");

        // when
        var response = kakaoSignupCommandService.signup(KAKAO_SIGNUP_TOKEN, List.of(1L), "127.0.0.1");

        // then
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.signupToken()).isEqualTo("signup-token");
        verify(memberSocialAccountRepository, times(1)).save(any(MemberSocialAccount.class));
        verify(memberTermsAgreementRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("카카오 이메일이 빈 문자열(미제공)이면 Member.email은 null로 저장된다")
    void signup_blankEmail_savedAsNull() {
        // given
        Claims claims = validClaims("", "", "");
        given(jwtProvider.parseClaims(KAKAO_SIGNUP_TOKEN)).willReturn(claims);
        given(memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, PROVIDER_USER_ID))
                .willReturn(Optional.empty());
        given(memberRepository.save(any(Member.class))).willReturn(savedMember());
        given(jwtProvider.generateToken(eq("1"), any(Map.class), any(Duration.class))).willReturn("signup-token");

        // when
        kakaoSignupCommandService.signup(KAKAO_SIGNUP_TOKEN, List.of(1L), "127.0.0.1");

        // then
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getEmail()).isNull();
    }

    @Test
    @DisplayName("이미 연동된(PENDING) 카카오 계정이면 재가입 대신 signupToken만 재발급한다")
    void signup_reissueForExistingPendingMember() {
        // given
        Claims claims = validClaims("user@kakao.com", "환승러", "http://img");
        given(jwtProvider.parseClaims(KAKAO_SIGNUP_TOKEN)).willReturn(claims);
        given(memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, PROVIDER_USER_ID))
                .willReturn(Optional.of(socialAccount(1L)));
        given(memberRepository.findById(1L)).willReturn(Optional.of(pendingMember()));
        given(jwtProvider.generateToken(eq("1"), any(Map.class), any(Duration.class))).willReturn("reissued-token");

        // when
        var response = kakaoSignupCommandService.signup(KAKAO_SIGNUP_TOKEN, List.of(1L), "127.0.0.1");

        // then
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.signupToken()).isEqualTo("reissued-token");
        verify(memberRepository, never()).save(any());
        verify(memberSocialAccountRepository, never()).save(any());
        verify(memberTermsAgreementRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("이미 프로필 설정까지 완료된(ACTIVE) 카카오 계정으로 재가입 시도하면 예외가 발생한다")
    void signup_alreadyRegistered_activeMemberConflict() {
        // given
        Claims claims = validClaims("user@kakao.com", "환승러", "http://img");
        given(jwtProvider.parseClaims(KAKAO_SIGNUP_TOKEN)).willReturn(claims);
        given(memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, PROVIDER_USER_ID))
                .willReturn(Optional.of(socialAccount(1L)));
        given(memberRepository.findById(1L)).willReturn(Optional.of(activeMember()));

        // when & then
        assertThatThrownBy(() -> kakaoSignupCommandService.signup(KAKAO_SIGNUP_TOKEN, List.of(1L), "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.KAKAO_ACCOUNT_ALREADY_REGISTERED.getMessage());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("필수 약관을 동의하지 않으면 예외가 발생한다")
    void signup_requiredTermsNotAgreed() {
        // given
        Claims claims = validClaims("user@kakao.com", "환승러", "http://img");
        given(jwtProvider.parseClaims(KAKAO_SIGNUP_TOKEN)).willReturn(claims);
        given(memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, PROVIDER_USER_ID))
                .willReturn(Optional.empty());
        willThrow(new CustomException(TermsErrorCode.REQUIRED_TERMS_NOT_AGREED))
                .given(termsAgreementValidator).validate(List.of());

        // when & then
        assertThatThrownBy(() -> kakaoSignupCommandService.signup(KAKAO_SIGNUP_TOKEN, List.of(), "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(TermsErrorCode.REQUIRED_TERMS_NOT_AGREED.getMessage());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 약관 id가 포함되면 예외가 발생한다")
    void signup_termsNotFound() {
        // given
        Claims claims = validClaims("user@kakao.com", "환승러", "http://img");
        given(jwtProvider.parseClaims(KAKAO_SIGNUP_TOKEN)).willReturn(claims);
        given(memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, PROVIDER_USER_ID))
                .willReturn(Optional.empty());
        willThrow(new CustomException(TermsErrorCode.TERMS_NOT_FOUND))
                .given(termsAgreementValidator).validate(List.of(1L, 999L));

        // when & then
        assertThatThrownBy(() -> kakaoSignupCommandService.signup(KAKAO_SIGNUP_TOKEN, List.of(1L, 999L), "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(TermsErrorCode.TERMS_NOT_FOUND.getMessage());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("만료된 kakaoSignupToken이면 예외가 발생한다")
    void signup_expiredKakaoSignupToken() {
        // given
        given(jwtProvider.parseClaims(KAKAO_SIGNUP_TOKEN)).willThrow(new ExpiredJwtException(null, null, "expired"));

        // when & then
        assertThatThrownBy(() -> kakaoSignupCommandService.signup(KAKAO_SIGNUP_TOKEN, List.of(1L), "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.KAKAO_SIGNUP_TOKEN_EXPIRED.getMessage());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("위변조된 kakaoSignupToken이면 예외가 발생한다")
    void signup_invalidSignatureToken() {
        // given
        given(jwtProvider.parseClaims(KAKAO_SIGNUP_TOKEN)).willThrow(new JwtException("invalid signature"));

        // when & then
        assertThatThrownBy(() -> kakaoSignupCommandService.signup(KAKAO_SIGNUP_TOKEN, List.of(1L), "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_KAKAO_SIGNUP_TOKEN.getMessage());
    }

    @Test
    @DisplayName("purpose가 KAKAO_SIGNUP이 아니면 예외가 발생한다 (다른 토큰을 잘못 넣은 경우)")
    void signup_wrongPurposeToken() {
        // given
        Claims claims = mock(Claims.class);
        given(claims.get(KakaoSignupTokenClaims.PURPOSE_KEY, String.class)).willReturn("SIGNUP");
        given(jwtProvider.parseClaims(KAKAO_SIGNUP_TOKEN)).willReturn(claims);

        // when & then
        assertThatThrownBy(() -> kakaoSignupCommandService.signup(KAKAO_SIGNUP_TOKEN, List.of(1L), "127.0.0.1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_KAKAO_SIGNUP_TOKEN.getMessage());
    }
}