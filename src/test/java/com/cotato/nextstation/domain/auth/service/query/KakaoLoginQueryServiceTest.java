package com.cotato.nextstation.domain.auth.service.query;

import com.cotato.nextstation.domain.auth.client.KakaoOAuthClient;
import com.cotato.nextstation.domain.auth.client.dto.KakaoTokenResponse;
import com.cotato.nextstation.domain.auth.client.dto.KakaoUserInfoResponse;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.auth.service.AuthTokenIssuer;
import com.cotato.nextstation.domain.auth.service.IssuedTokens;
import com.cotato.nextstation.domain.auth.service.result.KakaoLoginResult;
import com.cotato.nextstation.domain.auth.service.result.KakaoLoginResultType;
import com.cotato.nextstation.domain.auth.util.KakaoSignupTokenClaims;
import com.cotato.nextstation.domain.member.entity.AuthProvider;
import com.cotato.nextstation.domain.member.entity.Gender;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberSocialAccount;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.member.repository.MemberSocialAccountRepository;
import com.cotato.nextstation.domain.member.service.command.MemberCommandService;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.jwt.JwtProvider;
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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class KakaoLoginQueryServiceTest {

    @InjectMocks
    private KakaoLoginQueryService kakaoLoginQueryService;

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberSocialAccountRepository memberSocialAccountRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthTokenIssuer authTokenIssuer;

    @Mock
    private MemberCommandService memberCommandService;

    private static final String CODE = "auth-code";
    private static final String KAKAO_ACCESS_TOKEN = "kakao-access-token";
    private static final String PROVIDER_USER_ID = "555";

    private KakaoTokenResponse kakaoTokenResponse() {
        return new KakaoTokenResponse(KAKAO_ACCESS_TOKEN, "bearer", 3600L, "kakao-refresh-token", "profile_nickname");
    }

    private KakaoUserInfoResponse userInfoWithConsent() {
        KakaoUserInfoResponse.KakaoAccount.Profile profile =
                new KakaoUserInfoResponse.KakaoAccount.Profile("환승러", "http://profile.image");
        KakaoUserInfoResponse.KakaoAccount account =
                new KakaoUserInfoResponse.KakaoAccount("user@kakao.com", true, true, profile);
        return new KakaoUserInfoResponse(555L, account);
    }

    private KakaoUserInfoResponse userInfoWithoutConsent() {
        return new KakaoUserInfoResponse(555L, null);
    }

    private void givenTokenExchangeSucceeds(KakaoUserInfoResponse userInfoResponse) {
        given(kakaoOAuthClient.exchangeToken(CODE)).willReturn(kakaoTokenResponse());
        given(kakaoOAuthClient.fetchUserInfo(KAKAO_ACCESS_TOKEN)).willReturn(userInfoResponse);
    }

    private Member pendingMember() {
        Member member = Member.builder().email(null).build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    private Member activeMember() {
        Member member = Member.builder().email(null).build();
        ReflectionTestUtils.setField(member, "id", 1L);
        member.completeProfile("환승러", null, Gender.UNSPECIFIED, LocalDate.of(2000, 1, 1));
        return member;
    }

    private Member withdrawnMember(LocalDateTime deletedAt) {
        Member member = activeMember();
        member.withdraw();
        ReflectionTestUtils.setField(member, "deletedAt", deletedAt);
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
    @DisplayName("처음 보는 카카오 계정이면 Member를 만들지 않고 kakaoSignupToken을 발급한다")
    void login_newMember() {
        // given
        givenTokenExchangeSucceeds(userInfoWithConsent());
        given(memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, PROVIDER_USER_ID))
                .willReturn(Optional.empty());
        given(jwtProvider.generateToken(eq(PROVIDER_USER_ID), any(Map.class), any(Duration.class)))
                .willReturn("kakao-signup-token");

        // when
        KakaoLoginResult result = kakaoLoginQueryService.login(CODE);

        // then
        assertThat(result.resultType()).isEqualTo(KakaoLoginResultType.NEW_MEMBER);
        assertThat(result.kakaoSignupToken()).isEqualTo("kakao-signup-token");
        assertThat(result.kakaoNickname()).isEqualTo("환승러");
        assertThat(result.kakaoProfileImageUrl()).isEqualTo("http://profile.image");
        assertThat(result.memberId()).isNull();
        assertThat(result.accessToken()).isNull();
    }

    @Test
    @DisplayName("동의항목을 전부 거부한 신규 회원이어도 NPE 없이 kakaoSignupToken을 발급한다")
    void login_newMember_noConsent() {
        // given
        givenTokenExchangeSucceeds(userInfoWithoutConsent());
        given(memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, PROVIDER_USER_ID))
                .willReturn(Optional.empty());
        given(jwtProvider.generateToken(eq(PROVIDER_USER_ID), any(Map.class), any(Duration.class)))
                .willReturn("kakao-signup-token");

        // when
        KakaoLoginResult result = kakaoLoginQueryService.login(CODE);

        // then
        assertThat(result.resultType()).isEqualTo(KakaoLoginResultType.NEW_MEMBER);
        assertThat(result.kakaoNickname()).isNull();
        assertThat(result.kakaoProfileImageUrl()).isNull();

        // Map.of()는 value가 null이면 NPE를 던지므로, claim에는 빈 문자열로 들어갔는지 확인
        ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(jwtProvider).generateToken(eq(PROVIDER_USER_ID), claimsCaptor.capture(), any(Duration.class));
        assertThat(claimsCaptor.getValue().get(KakaoSignupTokenClaims.EMAIL_KEY)).isEqualTo("");
        assertThat(claimsCaptor.getValue().get(KakaoSignupTokenClaims.NICKNAME_KEY)).isEqualTo("");
        assertThat(claimsCaptor.getValue().get(KakaoSignupTokenClaims.PROFILE_IMAGE_URL_KEY)).isEqualTo("");
    }

    @Test
    @DisplayName("프로필 설정이 끝나지 않은(PENDING) 카카오 회원이 재로그인하면 signupToken을 재발급한다")
    void login_pendingMember() {
        // given
        givenTokenExchangeSucceeds(userInfoWithConsent());
        given(memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, PROVIDER_USER_ID))
                .willReturn(Optional.of(socialAccount(1L)));
        given(memberRepository.findById(1L)).willReturn(Optional.of(pendingMember()));
        given(jwtProvider.generateToken(eq("1"), any(Map.class), any(Duration.class)))
                .willReturn("reissued-signup-token");

        // when
        KakaoLoginResult result = kakaoLoginQueryService.login(CODE);

        // then
        assertThat(result.resultType()).isEqualTo(KakaoLoginResultType.PENDING_PROFILE);
        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.signupToken()).isEqualTo("reissued-signup-token");
    }

    @Test
    @DisplayName("ACTIVE 카카오 회원이 로그인하면 access token과 refresh token을 발급한다")
    void login_activeMember_loginSuccess() {
        // given
        givenTokenExchangeSucceeds(userInfoWithConsent());
        given(memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, PROVIDER_USER_ID))
                .willReturn(Optional.of(socialAccount(1L)));
        given(memberRepository.findById(1L)).willReturn(Optional.of(activeMember()));
        given(authTokenIssuer.issue(1L)).willReturn(new IssuedTokens("access-token", "refresh-token"));

        // when
        KakaoLoginResult result = kakaoLoginQueryService.login(CODE);

        // then
        assertThat(result.resultType()).isEqualTo(KakaoLoginResultType.LOGIN_SUCCESS);
        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("유예 기간이 지난 탈퇴 회원이면 복구하지 않고 예외가 발생한다")
    void login_memberNotActive() {
        // given - 8일 전 탈퇴 (유예 7일 경과)
        givenTokenExchangeSucceeds(userInfoWithConsent());
        given(memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, PROVIDER_USER_ID))
                .willReturn(Optional.of(socialAccount(1L)));
        given(memberRepository.findById(1L)).willReturn(Optional.of(withdrawnMember(LocalDateTime.now().minusDays(8))));

        // when & then
        assertThatThrownBy(() -> kakaoLoginQueryService.login(CODE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.KAKAO_MEMBER_NOT_ACTIVE.getMessage());

        then(memberCommandService).should(never()).restore(any());
    }

    @Test
    @DisplayName("유예 기간이 남은 탈퇴 회원이 카카오로 로그인하면 계정을 복구하고 토큰을 발급한다")
    void login_restoresWithdrawnMemberWithinGracePeriod() {
        // given - 3일 전 탈퇴
        givenTokenExchangeSucceeds(userInfoWithConsent());
        given(memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, PROVIDER_USER_ID))
                .willReturn(Optional.of(socialAccount(1L)));
        given(memberRepository.findById(1L)).willReturn(Optional.of(withdrawnMember(LocalDateTime.now().minusDays(3))));
        given(memberCommandService.restore(1L)).willReturn(MemberStatus.ACTIVE);
        given(authTokenIssuer.issue(1L)).willReturn(new IssuedTokens("access-token", "refresh-token"));

        // when
        KakaoLoginResult result = kakaoLoginQueryService.login(CODE);

        // then
        assertThat(result.resultType()).isEqualTo(KakaoLoginResultType.LOGIN_SUCCESS);
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.restored()).isTrue();
    }

    @Test
    @DisplayName("member_social_account는 있는데 member가 없으면(데이터 정합성 오류) 예외가 발생한다")
    void login_memberNotFound_dataIntegrityError() {
        // given
        givenTokenExchangeSucceeds(userInfoWithConsent());
        given(memberSocialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, PROVIDER_USER_ID))
                .willReturn(Optional.of(socialAccount(999L)));
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> kakaoLoginQueryService.login(CODE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.MEMBER_NOT_FOUND.getMessage());
    }
}