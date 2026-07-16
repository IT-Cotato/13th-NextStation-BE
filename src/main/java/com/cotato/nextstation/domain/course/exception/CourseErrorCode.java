package com.cotato.nextstation.domain.course.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CourseErrorCode implements ErrorCode {

    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_COURSE_NOT_FOUND", "존재하지 않는 코스입니다."),
    INVALID_COURSE_PLACES(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_INVALID_COURSE_PLACES", "코스 장소 구성이 올바르지 않습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
