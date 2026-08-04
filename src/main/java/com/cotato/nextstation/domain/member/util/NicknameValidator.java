package com.cotato.nextstation.domain.member.util;

import com.cotato.nextstation.domain.member.exception.NicknameErrorCode;
import com.cotato.nextstation.domain.member.repository.MemberRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class NicknameValidator {

    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 10;
    private static final Pattern NICKNAME_ALLOWED_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]+$");

    private final MemberRepository memberRepository;
    private final NicknameProfanityFilter nicknameProfanityFilter;
    private final NicknameReservedWordsFilter nicknameReservedWordsFilter;

    // null -> 길이 -> 허용 문자 -> 예약어 -> 금칙어 -> 중복 순으로 검증
    // 여러 호출부가 공유하는 컴포넌트라 null 방어는 호출부에 맡기지 않고 여기서 직접 한다.
    public void validate(String nickname) {
        if (nickname == null || nickname.length() < NICKNAME_MIN_LENGTH) {
            throw new CustomException(NicknameErrorCode.NICKNAME_TOO_SHORT);
        }
        if (nickname.length() > NICKNAME_MAX_LENGTH) {
            throw new CustomException(NicknameErrorCode.NICKNAME_TOO_LONG);
        }
        if (!NICKNAME_ALLOWED_PATTERN.matcher(nickname).matches()) {
            throw new CustomException(NicknameErrorCode.NICKNAME_INVALID_CHARACTER);
        }
        if (nicknameReservedWordsFilter.isReservedWord(nickname)) {
            log.warn("예약어가 포함된 닉네임: nickname={}", nickname);
            throw new CustomException(NicknameErrorCode.NICKNAME_CONTAINS_RESERVED_WORD);
        }
        if (nicknameProfanityFilter.containsBannedWord(nickname)) {
            log.warn("금칙어가 포함된 닉네임: nickname={}", nickname);
            throw new CustomException(NicknameErrorCode.NICKNAME_CONTAINS_BANNED_WORD);
        }
        if (memberRepository.existsByNickname(nickname)) {
            log.warn("이미 사용 중인 닉네임: nickname={}", nickname);
            throw new CustomException(NicknameErrorCode.DUPLICATE_NICKNAME);
        }
    }
}