package com.cotato.nextstation.domain.recommendation.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecommendationErrorCode implements ErrorCode {

    NO_DRAWABLE_STATION(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_NO_DRAWABLE_STATION", "추천할 수 있는 역이 없습니다."),
    DEPARTURE_STATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_DEPARTURE_STATION_NOT_FOUND", "존재하지 않는 출발역입니다."),
    NO_REACHABLE_STATION(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_NO_REACHABLE_STATION", "조건에 맞게 갈 수 있는 역이 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
