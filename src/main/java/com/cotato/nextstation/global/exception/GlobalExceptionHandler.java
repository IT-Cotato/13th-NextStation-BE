package com.cotato.nextstation.global.exception;

import com.cotato.nextstation.global.common.response.CommonResponse;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 검증 에러 (MethodArgumentNotValidException)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> reasons = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                reasons.put(error.getField(), error.getDefaultMessage())
        );
        return CommonResponse.error(GlobalErrorCode.VALIDATION_ERROR, reasons);
    }

    /**
     * 커스텀 예외
     */
    @ExceptionHandler(CustomException.class)
    public CommonResponse<Void> handleCustomException(CustomException ex) {
        return CommonResponse.error(ex.getErrorCode());
    }

    /**
     * 기타 모든 예외
     */
    @ExceptionHandler(Exception.class)
    public CommonResponse<Void> handleGeneralException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return CommonResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }
}