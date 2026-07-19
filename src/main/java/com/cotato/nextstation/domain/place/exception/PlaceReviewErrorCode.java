package com.cotato.nextstation.domain.place.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaceReviewErrorCode implements ErrorCode {

    PLACE_REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_PLACE_REVIEW_NOT_FOUND", "존재하지 않는 리뷰입니다."),
    PLACE_REVIEW_LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "CLIENT_ERROR_409_PLACE_REVIEW_LIKE_ALREADY_EXISTS", "이미 좋아요를 누른 리뷰입니다."),
    PLACE_REVIEW_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_PLACE_REVIEW_LIKE_NOT_FOUND", "좋아요를 누르지 않은 리뷰입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}