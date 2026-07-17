package com.cotato.nextstation.domain.auth.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "CLIENT_ERROR_409_DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
    EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "CLIENT_ERROR_429_EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED", "인증번호 요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),
    EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_EMAIL_VERIFICATION_NOT_FOUND", "유효한 인증번호 발송 내역이 없습니다. 인증번호를 다시 요청해주세요."),
    EMAIL_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_EMAIL_VERIFICATION_EXPIRED", "인증번호가 만료되었습니다. 인증번호를 다시 요청해주세요."),
    EMAIL_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_EMAIL_VERIFICATION_CODE_MISMATCH", "인증번호가 일치하지 않습니다."),
    EMAIL_VERIFICATION_ATTEMPT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "CLIENT_ERROR_429_EMAIL_VERIFICATION_ATTEMPT_EXCEEDED", "인증번호 확인 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}