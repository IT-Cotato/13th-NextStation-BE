package com.cotato.nextstation.domain.place.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {

    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_PLACE_NOT_FOUND", "존재하지 않는 장소입니다."),
    PLACE_REVIEW_NOT_FOUND(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_PLACE_REVIEW_NOT_FOUND", "해당 여행일지에 존재하지 않는 장소의 리뷰는 수정할 수 없습니다")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}