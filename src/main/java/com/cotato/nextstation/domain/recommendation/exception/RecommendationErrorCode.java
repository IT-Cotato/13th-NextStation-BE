package com.cotato.nextstation.domain.recommendation.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecommendationErrorCode implements ErrorCode {

    NO_DRAWABLE_STATION(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_NO_DRAWABLE_STATION", "추천할 수 있는 역이 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
