package com.cotato.nextstation.domain.member.util;

import com.cotato.nextstation.domain.member.exception.NicknameErrorCode;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class NicknameValidatorTest {

    @InjectMocks
    private NicknameValidator nicknameValidator;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NicknameProfanityFilter nicknameProfanityFilter;

    @Mock
    private NicknameReservedWordsFilter nicknameReservedWordsFilter;

    private static final String NICKNAME = "환승러";

    @Test
    @DisplayName("규칙을 전부 통과하면 예외 없이 검증이 끝난다")
    void validate_success() {
        // given
        lenient().when(memberRepository.existsByNickname(anyString())).thenReturn(false);

        // when & then
        assertThatCode(() -> nicknameValidator.validate(NICKNAME)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("닉네임이 null이면 예외가 발생한다")
    void validate_null() {
        assertThatThrownBy(() -> nicknameValidator.validate(null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.NICKNAME_TOO_SHORT.getMessage());
    }

    @Test
    @DisplayName("닉네임이 2자 미만이면 예외가 발생한다")
    void validate_tooShort() {
        assertThatThrownBy(() -> nicknameValidator.validate("환"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.NICKNAME_TOO_SHORT.getMessage());
    }

    @Test
    @DisplayName("닉네임이 10자를 초과하면 예외가 발생한다")
    void validate_tooLong() {
        assertThatThrownBy(() -> nicknameValidator.validate("환승러환승러환승러환승러"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.NICKNAME_TOO_LONG.getMessage());
    }

    @Test
    @DisplayName("허용되지 않은 문자가 포함되면 예외가 발생한다")
    void validate_invalidCharacter() {
        assertThatThrownBy(() -> nicknameValidator.validate("환승러!!"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.NICKNAME_INVALID_CHARACTER.getMessage());
    }

    @Test
    @DisplayName("예약어가 포함되거나 일치하면 예외가 발생한다")
    void validate_reservedWord() {
        given(nicknameReservedWordsFilter.isReservedWord("운영자1")).willReturn(true);

        assertThatThrownBy(() -> nicknameValidator.validate("운영자1"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.NICKNAME_CONTAINS_RESERVED_WORD.getMessage());
    }

    @Test
    @DisplayName("금칙어가 포함되면 예외가 발생한다")
    void validate_bannedWord() {
        given(nicknameReservedWordsFilter.isReservedWord(NICKNAME)).willReturn(false);
        given(nicknameProfanityFilter.containsBannedWord(NICKNAME)).willReturn(true);

        assertThatThrownBy(() -> nicknameValidator.validate(NICKNAME))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.NICKNAME_CONTAINS_BANNED_WORD.getMessage());
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임이면 예외가 발생한다")
    void validate_duplicateNickname() {
        given(nicknameReservedWordsFilter.isReservedWord(NICKNAME)).willReturn(false);
        given(nicknameProfanityFilter.containsBannedWord(NICKNAME)).willReturn(false);
        given(memberRepository.existsByNickname(NICKNAME)).willReturn(true);

        assertThatThrownBy(() -> nicknameValidator.validate(NICKNAME))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(NicknameErrorCode.DUPLICATE_NICKNAME.getMessage());
    }
}
