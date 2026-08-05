package com.cotato.nextstation.domain.member.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

    // 400
    INVALID_PROFILE_IMAGE_URL(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_INVALID_PROFILE_IMAGE_URL", "허용되지 않은 프로필 이미지 URL입니다."),

    // 404
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_MEMBER_NOT_FOUND", "존재하지 않는 회원입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}