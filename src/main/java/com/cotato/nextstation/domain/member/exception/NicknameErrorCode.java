package com.cotato.nextstation.domain.member.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NicknameErrorCode implements ErrorCode {

    // 400
    NICKNAME_TOO_SHORT(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_NICKNAME_TOO_SHORT", "닉네임은 2자 이상 입력해주세요."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_NICKNAME_TOO_LONG", "닉네임은 최대 10자까지 입력할 수 있어요."),
    NICKNAME_INVALID_CHARACTER(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_NICKNAME_INVALID_CHARACTER", "한글, 영문, 숫자만 사용할 수 있어요."),
    NICKNAME_CONTAINS_BANNED_WORD(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_NICKNAME_CONTAINS_BANNED_WORD", "사용할 수 없는 단어가 포함되어 있어요."),
    NICKNAME_CONTAINS_RESERVED_WORD(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_NICKNAME_CONTAINS_RESERVED_WORD", "운영자, 관리자 등 예약어는 닉네임으로 사용할 수 없어요."),

    // 409
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "CLIENT_ERROR_409_DUPLICATE_NICKNAME", "이미 사용 중인 닉네임이에요."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}