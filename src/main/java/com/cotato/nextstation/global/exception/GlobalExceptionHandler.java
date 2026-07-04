package com.cotato.nextstation.global.exception;

import com.cotato.nextstation.global.common.response.CommonResponse;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 검증 에러 (MethodArgumentNotValidException) -> 400 Bad Request 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> reasons = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                reasons.put(error.getField(), error.getDefaultMessage())
        );

        CommonResponse<Void> response = CommonResponse.error(GlobalErrorCode.VALIDATION_ERROR, reasons);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 커스텀 예외 -> 에러 코드에 매핑된 HTTP 상태 코드 반환
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CommonResponse<Void>> handleCustomException(CustomException ex) {
        log.warn("CustomException: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        HttpStatus status = ex.getErrorCode().getHttpStatus();

        CommonResponse<Void> response = CommonResponse.error(ex.getErrorCode());
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 기타 모든 예외 -> 500 Internal Server Error 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Unexpected error: ", ex);

        CommonResponse<Void> response = CommonResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}