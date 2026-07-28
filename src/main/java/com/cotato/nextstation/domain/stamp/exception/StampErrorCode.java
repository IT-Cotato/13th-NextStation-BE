package com.cotato.nextstation.domain.stamp.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StampErrorCode implements ErrorCode {

    STATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_PLACE_NOT_FOUND", "존재하지 않는 역입니다."),
    COURSE_NOT_OWNED(HttpStatus.FORBIDDEN, "CLIENT_ERROR_403_COURSE_NOT_OWNED", "본인 코스만 완료할 수 있습니다."),

    DUPLICATE_COURSE_COMPLETE(HttpStatus.CONFLICT, "CLIENT_ERROR_409_DUPLICATE_COURSE_COMPLETE", "이미 완료한 코스입니다."),;


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}