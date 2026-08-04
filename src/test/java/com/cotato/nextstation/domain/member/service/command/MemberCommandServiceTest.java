package com.cotato.nextstation.domain.member.service.command;

import com.cotato.nextstation.domain.image.service.command.ImageCommandService;
import com.cotato.nextstation.domain.member.converter.MemberConverter;
import com.cotato.nextstation.domain.member.dto.response.MemberProfileResponse;
import com.cotato.nextstation.domain.member.entity.Member;
import com.cotato.nextstation.domain.member.exception.MemberErrorCode;
import com.cotato.nextstation.domain.member.exception.NicknameErrorCode;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.domain.member.util.NicknameValidator;
import com.cotato.nextstation.domain.member.util.ProfileImageUrlValidator;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberCommandServiceTest {

    @InjectMocks
    private MemberCommandService memberCommandService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberConverter memberConverter;

    @Mock
    private NicknameValidator nicknameValidator;

    @Mock
    private ProfileImageUrlValidator profileImageUrlValidator;

    @Mock
    private ImageCommandService imageCommandService;

    private static final Long MEMBER_ID = 1L;
    private static final String OLD_IMAGE_URL =
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/uploads/profile/1/old.jpg";
    private static final String NEW_IMAGE_URL =
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/uploads/profile/1/new.jpg";

    private Member memberWith(String nickname, String profileImageUrl) {
        Member member = Member.builder().email("user@example.com").password("encoded").build();
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        member.completeProfile(nickname, profileImageUrl, member.getGender(), null);
        return member;
    }

    @BeforeEach
    void setUp() {
        lenient().when(memberConverter.toProfileResponse(any(Member.class)))
                .thenAnswer(invocation -> {
                    Member member = invocation.getArgument(0);
                    return new MemberProfileResponse(member.getId(), member.getNickname(), member.getProfileImageUrl());
                });
    }

    @Test
    @DisplayName("nickname만 보내면 닉네임만 바뀌고 프로필 이미지는 그대로다")
    void updateMyProfile_nicknameOnly() {
        // given
        Member member = memberWith("기존닉네임", OLD_IMAGE_URL);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        // when
        MemberProfileResponse response = memberCommandService.updateMyProfile(MEMBER_ID, "새닉네임", null);

        // then
        assertThat(response.nickname()).isEqualTo("새닉네임");
        assertThat(response.profileImageUrl()).isEqualTo(OLD_IMAGE_URL);
        verify(nicknameValidator).validate("새닉네임");
        verify(imageCommandService, never()).deleteImage(anyString(), anyLong());
    }

    @Test
    @DisplayName("nickname을 현재 값과 동일하게 보내면 검증 없이 통과한다")
    void updateMyProfile_sameNicknameSkipsValidation() {
        // given
        Member member = memberWith("기존닉네임", null);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        // when
        memberCommandService.updateMyProfile(MEMBER_ID, "기존닉네임", null);

        // then
        verify(nicknameValidator, never()).validate(anyString());
    }

    @Test
    @DisplayName("profileImageUrl을 새 값으로 보내면 검증 후 교체되고 기존 이미지는 삭제된다")
    void updateMyProfile_replaceImage() {
        // given
        Member member = memberWith("닉네임", OLD_IMAGE_URL);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        // when
        MemberProfileResponse response = memberCommandService.updateMyProfile(MEMBER_ID, null, NEW_IMAGE_URL);

        // then
        assertThat(response.profileImageUrl()).isEqualTo(NEW_IMAGE_URL);
        verify(profileImageUrlValidator).validate(NEW_IMAGE_URL, MEMBER_ID);
        verify(imageCommandService).deleteImage(OLD_IMAGE_URL, MEMBER_ID);
    }

    @Test
    @DisplayName("profileImageUrl을 빈 문자열로 보내면 이미지가 제거되고 기존 이미지는 삭제된다")
    void updateMyProfile_removeImage() {
        // given
        Member member = memberWith("닉네임", OLD_IMAGE_URL);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        // when
        MemberProfileResponse response = memberCommandService.updateMyProfile(MEMBER_ID, null, "");

        // then
        assertThat(response.profileImageUrl()).isNull();
        verify(imageCommandService).deleteImage(OLD_IMAGE_URL, MEMBER_ID);
    }

    @Test
    @DisplayName("허용되지 않은 프로필 이미지 URL이면 예외가 발생하고 기존 이미지는 삭제되지 않는다")
    void updateMyProfile_invalidImageUrl() {
        // given
        Member member = memberWith("닉네임", OLD_IMAGE_URL);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        doThrow(new CustomException(MemberErrorCode.INVALID_PROFILE_IMAGE_URL))
                .when(profileImageUrlValidator).validate(anyString(), eq(MEMBER_ID));

        // when & then
        assertThatThrownBy(() -> memberCommandService.updateMyProfile(MEMBER_ID, null, "https://evil.com/xss.svg"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(MemberErrorCode.INVALID_PROFILE_IMAGE_URL.getMessage());
        verify(imageCommandService, never()).deleteImage(anyString(), anyLong());
    }

    @Test
    @DisplayName("닉네임 검증에서 예외가 발생하면 그대로 전파된다")
    void updateMyProfile_duplicateNickname() {
        // given
        Member member = memberWith("기존닉네임", null);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
        doThrow(new CustomException(NicknameErrorCode.DUPLICATE_NICKNAME))
                .when(nicknameValidator).validate("중복닉네임");

        // when & then
        assertThatThrownBy(() -> memberCommandService.updateMyProfile(MEMBER_ID, "중복닉네임", null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.DUPLICATE_NICKNAME.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 예외가 발생한다")
    void updateMyProfile_memberNotFound() {
        // given
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberCommandService.updateMyProfile(MEMBER_ID, "닉네임", null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
    }
}