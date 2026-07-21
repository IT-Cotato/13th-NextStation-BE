package com.cotato.nextstation.domain.stamp.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StampErrorCode implements ErrorCode {

    STATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_PLACE_NOT_FOUND", "존재하지 않는 역입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}