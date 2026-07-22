package com.cotato.nextstation.domain.course.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CourseErrorCode implements ErrorCode {

    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_COURSE_NOT_FOUND", "존재하지 않는 코스입니다."),
    COURSE_FORBIDDEN(HttpStatus.FORBIDDEN, "CLIENT_ERROR_403_COURSE_FORBIDDEN", "본인 코스만 수정할 수 있습니다."),
    DUPLICATE_COURSE_PLACES(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_DUPLICATE_COURSE_PLACES", "같은 장소를 중복으로 선택할 수 없습니다."),
    INVALID_COURSE_PLACES(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_INVALID_COURSE_PLACES", "장소 목록이 기존 코스 구성과 일치하지 않습니다."),
    DUPLICATE_COURSE_SAVE(HttpStatus.CONFLICT, "CLIENT_ERROR_409_DUPLICATE_COURSE_SAVE", "이미 저장한 코스입니다."),
    COURSE_SAVE_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_COURSE_SAVE_NOT_FOUND", "저장하지 않은 코스입니다."),
    COURSE_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "CLIENT_ERROR_403_COURSE_DELETE_FORBIDDEN", "본인 코스만 삭제할 수 있습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
