package com.cotato.nextstation.domain.auth.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TermsErrorCode implements ErrorCode {

    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_TERMS_NOT_FOUND", "존재하지 않는 약관입니다."),
    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_REQUIRED_TERMS_NOT_AGREED", "필수 약관에 동의해야 합니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}