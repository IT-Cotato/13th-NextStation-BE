package com.cotato.nextstation.domain.journal.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum JournalErrorCode implements ErrorCode {

    JOURNAL_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_JOURNAL_NOT_FOUND", "존재하지 않는 여행일지입니다."),
    JOURNAL_FORBIDDEN(HttpStatus.FORBIDDEN, "CLIENT_ERROR_403_JOURNAL_FORBIDDEN", "본인 여행일지만 수정/삭제할 수 있습니다."),
    MEMBER_STAMP_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_ERROR_404_MEMBER_STAMP_NOT_FOUND", "존재하지 않는 스탬프입니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}