package com.cotato.nextstation.global.common.response;

import com.cotato.nextstation.global.exception.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {

    private final boolean success;
    private final int status;
    private final String code;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;
    private final Map<String, Object> reasons;

    private CommonResponse(
            boolean success,
            int status,
            String code,
            String message,
            T data,
            LocalDateTime timestamp,
            Map<String, Object> reasons
    ) {
        this.success = success;
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
        this.reasons = reasons;
    }

    // 성공 응답
    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(
                true,
                HttpStatus.OK.value(),
                "SUCCESS",
                "요청이 성공적으로 처리되었습니다.",
                data,
                LocalDateTime.now(),
                null
        );
    }

    // 에러 응답
    public static CommonResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode, null);
    }

    public static CommonResponse<Void> error(ErrorCode errorCode, Map<String, Object> reasons) {
        return new CommonResponse<>(
                false,
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                null,
                LocalDateTime.now(),
                reasons
        );
    }
}