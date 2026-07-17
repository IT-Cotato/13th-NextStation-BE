package com.cotato.nextstation.domain.place.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {

    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_PLACE_NOT_FOUND", "존재하지 않는 장소입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}