package com.cotato.nextstation.global.exception.support;

import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExceptionTestController {

    @GetMapping("/test/custom")
    public void triggerCustomException() {
        throw new CustomException(GlobalErrorCode.NOT_FOUND);
    }

    @PostMapping("/test/validate")
    public void triggerValidationException(@Valid @RequestBody TestRequest request) {
    }

    @GetMapping("/test/required-param")
    public void triggerMissingParam(@RequestParam String name) {
    }

    @GetMapping("/test/type-mismatch")
    public void triggerTypeMismatch(@RequestParam int number) {
    }

    public record TestRequest(@NotBlank String name) {
    }
}