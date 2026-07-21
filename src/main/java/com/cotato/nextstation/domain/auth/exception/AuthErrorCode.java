package com.cotato.nextstation.domain.auth.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    // 400
    EMAIL_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_EMAIL_VERIFICATION_EXPIRED", "인증번호가 만료되었습니다. 인증번호를 다시 요청해주세요."),
    EMAIL_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_EMAIL_VERIFICATION_CODE_MISMATCH", "인증번호가 일치하지 않습니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_EMAIL_NOT_VERIFIED", "이메일 인증이 완료되지 않았습니다."),
    PASSWORD_CONFIRMATION_MISMATCH(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_PASSWORD_CONFIRMATION_MISMATCH", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    INVALID_PROFILE_IMAGE_URL(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_INVALID_PROFILE_IMAGE_URL", "허용되지 않은 프로필 이미지 URL입니다."),

    // 401
    INVALID_SIGNUP_TOKEN(HttpStatus.UNAUTHORIZED, "CLIENT_ERROR_401_INVALID_SIGNUP_TOKEN", "유효하지 않은 토큰입니다."),
    SIGNUP_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "CLIENT_ERROR_401_SIGNUP_TOKEN_EXPIRED", "토큰이 만료되었습니다. 회원가입을 다시 진행해주세요."),
    PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "CLIENT_ERROR_401_PASSWORD_MISMATCH", "비밀번호가 일치하지 않습니다."),

    // 404
    EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_EMAIL_VERIFICATION_NOT_FOUND", "유효한 인증번호 발송 내역이 없습니다. 인증번호를 다시 요청해주세요."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_MEMBER_NOT_FOUND", "존재하지 않는 회원입니다."),

    // 409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "CLIENT_ERROR_409_DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
    PROFILE_ALREADY_COMPLETED(HttpStatus.CONFLICT, "CLIENT_ERROR_409_PROFILE_ALREADY_COMPLETED", "이미 프로필 설정이 완료된 회원입니다."),

    // 429
    EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "CLIENT_ERROR_429_EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED", "인증번호 요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),
    EMAIL_VERIFICATION_ATTEMPT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "CLIENT_ERROR_429_EMAIL_VERIFICATION_ATTEMPT_EXCEEDED", "인증번호 확인 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}