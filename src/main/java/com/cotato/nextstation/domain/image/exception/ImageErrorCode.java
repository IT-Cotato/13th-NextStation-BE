package com.cotato.nextstation.domain.image.exception;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

    // 400
    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_INVALID_FILE_NAME", "파일명에 확장자가 없습니다."),
    UNSUPPORTED_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_UNSUPPORTED_FILE_EXTENSION", "지원하지 않는 이미지 확장자입니다. (jpg, jpeg, png, webp, gif만 허용)"),
    MISSING_MEMBER_ID(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_MISSING_MEMBER_ID", "memberId가 필요합니다."),
    MISSING_JOURNAL_ID(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_MISSING_JOURNAL_ID", "여행일지 이미지 업로드에는 journalId가 필요합니다."),
    UNSUPPORTED_UPLOAD_FOLDER(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_UNSUPPORTED_UPLOAD_FOLDER", "presigned URL 발급 대상이 아닌 폴더입니다."),
    PROFILE_NOT_ALLOWED_IN_BATCH(HttpStatus.BAD_REQUEST, "CLIENT_ERROR_400_PROFILE_NOT_ALLOWED_IN_BATCH", "프로필 이미지는 단일 업로드 API를 사용해주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}