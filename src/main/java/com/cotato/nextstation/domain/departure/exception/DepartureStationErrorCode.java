package com.cotato.nextstation.domain.departure.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DepartureStationErrorCode implements ErrorCode {

    MAX_DEPARTURE_STATIONS_EXCEEDED(HttpStatus.CONFLICT, "CLIENT_ERROR_409_MAX_DEPARTURE_STATIONS_EXCEEDED", "출발역은 최대 10개까지 저장할 수 있습니다."),
    DUPLICATE_DEPARTURE_STATION(HttpStatus.CONFLICT, "CLIENT_ERROR_409_DUPLICATE_DEPARTURE_STATION", "이미 추가한 출발역입니다."),
    DEPARTURE_STATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_DEPARTURE_STATION_NOT_FOUND", "존재하지 않는 출발역 즐겨찾기입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
