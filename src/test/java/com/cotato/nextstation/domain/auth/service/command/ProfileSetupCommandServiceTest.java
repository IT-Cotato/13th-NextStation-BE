package com.cotato.nextstation.domain.auth.service.command;

import com.cotato.nextstation.domain.auth.dto.response.ProfileSetupResponse;
import com.cotato.nextstation.domain.auth.exception.AuthErrorCode;
import com.cotato.nextstation.domain.member.entity.Gender;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.entity.MemberStatus;
import com.cotato.nextstation.domain.member.exception.NicknameErrorCode;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.member.util.NicknameProfanityFilter;
import com.cotato.nextstation.domain.member.util.NicknameReservedWordsFilter;
import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ProfileSetupCommandServiceTest {

    private ProfileSetupCommandService profileSetupCommandService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NicknameProfanityFilter nicknameProfanityFilter;

    @Mock
    private NicknameReservedWordsFilter nicknameReservedWordsFilter;

    @Mock
    private JwtProvider jwtProvider;

    private static final Long MEMBER_ID = 1L;
    private static final String NICKNAME = "환승러";
    private static final Gender GENDER = Gender.MALE;
    private static final LocalDate BIRTH_DATE = LocalDate.of(2001, 1, 1);
    private static final String TOKEN = "signup-token";
    private static final String AUTH_HEADER = "Bearer " + TOKEN;
    private static final String VALID_PROFILE_IMAGE_URL =
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/uploads/profile/1/uuid.jpg";

    // @Value 생성자 파라미터는 목으로 대체할 수 없어 직접 값을 넣어 생성한다
    @BeforeEach
    void setUp() {
        profileSetupCommandService = new ProfileSetupCommandService(
                memberRepository, nicknameProfanityFilter, nicknameReservedWordsFilter, jwtProvider, "test-bucket", "ap-northeast-2");
    }

    private Member pendingMember() {
        Member member = Member.builder().email("user@example.com").password("encoded").build();
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    private Claims signupClaims(Long memberId) {
        return Jwts.claims().subject(memberId.toString()).add("purpose", "SIGNUP").build();
    }

    private void givenValidToken() {
        given(jwtProvider.parseClaims(TOKEN)).willReturn(signupClaims(MEMBER_ID));
    }

    @Test
    @DisplayName("정상 요청이면 프로필이 설정되고 status가 ACTIVE로 전환된다")
    void setupProfile_success() {
        // given
        givenValidToken();
        Member member = pendingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(memberRepository.existsByNickname(NICKNAME)).willReturn(false);

        // when
        ProfileSetupResponse response = profileSetupCommandService.setupProfile(
                AUTH_HEADER, NICKNAME, VALID_PROFILE_IMAGE_URL, GENDER, BIRTH_DATE);

        // then
        assertThat(response.memberId()).isEqualTo(MEMBER_ID);
        assertThat(response.nickname()).isEqualTo(NICKNAME);
        assertThat(response.status()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getGender()).isEqualTo(GENDER);
        assertThat(member.getBirthDate()).isEqualTo(BIRTH_DATE);
        assertThat(member.getProfileImageUrl()).isEqualTo(VALID_PROFILE_IMAGE_URL);
    }

    @Test
    @DisplayName("본인 S3 버킷 경로가 아닌 프로필 이미지 URL이면 예외가 발생한다")
    void setupProfile_invalidProfileImageUrl() {
        // given
        givenValidToken();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(pendingMember()));
        given(memberRepository.existsByNickname(NICKNAME)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(
                AUTH_HEADER, NICKNAME, "https://evil.com/xss.svg", GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_PROFILE_IMAGE_URL.getMessage());
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 예외가 발생한다")
    void setupProfile_missingAuthorizationHeader() {
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(null, NICKNAME, null, GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_SIGNUP_TOKEN.getMessage());
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 예외가 발생한다")
    void setupProfile_notBearerFormat() {
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(TOKEN, NICKNAME, null, GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_SIGNUP_TOKEN.getMessage());
    }

    @Test
    @DisplayName("만료된 토큰이면 예외가 발생한다")
    void setupProfile_expiredToken() {
        // given
        given(jwtProvider.parseClaims(TOKEN)).willThrow(mock(ExpiredJwtException.class));

        // when & then
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(AUTH_HEADER, NICKNAME, null, GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.SIGNUP_TOKEN_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("위변조된 토큰이면 예외가 발생한다")
    void setupProfile_malformedToken() {
        // given
        given(jwtProvider.parseClaims(TOKEN)).willThrow(new MalformedJwtException("malformed"));

        // when & then
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(AUTH_HEADER, NICKNAME, null, GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_SIGNUP_TOKEN.getMessage());
    }

    @Test
    @DisplayName("purpose가 SIGNUP이 아니면 예외가 발생한다")
    void setupProfile_wrongPurpose() {
        // given
        Claims wrongPurposeClaims = Jwts.claims().subject(MEMBER_ID.toString()).add("purpose", "ACCESS").build();
        given(jwtProvider.parseClaims(TOKEN)).willReturn(wrongPurposeClaims);

        // when & then
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(AUTH_HEADER, NICKNAME, null, GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.INVALID_SIGNUP_TOKEN.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 예외가 발생한다")
    void setupProfile_memberNotFound() {
        // given
        givenValidToken();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(AUTH_HEADER, NICKNAME, null, GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("이미 프로필 설정이 완료된 회원(status != PENDING)이면 예외가 발생한다")
    void setupProfile_alreadyCompleted() {
        // given
        givenValidToken();
        Member member = pendingMember();
        member.completeProfile("기존닉네임", null, Gender.UNSPECIFIED, BIRTH_DATE);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(AUTH_HEADER, NICKNAME, null, GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(AuthErrorCode.PROFILE_ALREADY_COMPLETED.getMessage());
    }

    @Test
    @DisplayName("닉네임이 중복이면 예외가 발생한다")
    void setupProfile_duplicateNickname() {
        // given
        givenValidToken();
        Member member = pendingMember();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        given(memberRepository.existsByNickname(NICKNAME)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(AUTH_HEADER, NICKNAME, null, GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.DUPLICATE_NICKNAME.getMessage());
    }

    @Test
    @DisplayName("닉네임이 2자 미만이면 예외가 발생한다")
    void setupProfile_nicknameTooShort() {
        // given
        givenValidToken();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(pendingMember()));

        // when & then
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(AUTH_HEADER, "환", null, GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.NICKNAME_TOO_SHORT.getMessage());
    }

    @Test
    @DisplayName("닉네임이 10자를 초과하면 예외가 발생한다")
    void setupProfile_nicknameTooLong() {
        // given
        givenValidToken();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(pendingMember()));

        // when & then
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(AUTH_HEADER, "환승러환승러환승러환승러", null, GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.NICKNAME_TOO_LONG.getMessage());
    }

    @Test
    @DisplayName("닉네임에 허용되지 않은 문자가 포함되면 예외가 발생한다")
    void setupProfile_nicknameInvalidCharacter() {
        // given
        givenValidToken();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(pendingMember()));

        // when & then
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(AUTH_HEADER, "환승러!!", null, GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.NICKNAME_INVALID_CHARACTER.getMessage());
    }

    @Test
    @DisplayName("닉네임에 금칙어가 포함되면 예외가 발생한다")
    void setupProfile_containsBannedWord() {
        // given
        givenValidToken();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(pendingMember()));
        given(nicknameProfanityFilter.containsBannedWord(anyString())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(AUTH_HEADER, NICKNAME, null, GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.NICKNAME_CONTAINS_BANNED_WORD.getMessage());
    }

    @Test
    @DisplayName("닉네임에 예약어가 포함되거나 일치하면 예외가 발생한다")
    void setupProfile_containsReservedWord() {
        // given
        givenValidToken();
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(pendingMember()));
        given(nicknameReservedWordsFilter.isReservedWord(anyString())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> profileSetupCommandService.setupProfile(AUTH_HEADER, "운영자1", null, GENDER, BIRTH_DATE))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.NICKNAME_CONTAINS_RESERVED_WORD.getMessage());
    }
}