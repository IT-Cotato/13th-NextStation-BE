package com.cotato.nextstation.domain.auth.controller;

import com.cotato.nextstation.domain.auth.dto.request.EmailVerificationSendRequest;
import com.cotato.nextstation.domain.auth.service.EmailVerificationCommandService;
import com.cotato.nextstation.global.common.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final EmailVerificationCommandService emailVerificationCommandService;

    @Operation(
            summary = "회원가입 이메일 인증번호 발송",
            description = """
                    입력한 이메일로 6자리 인증번호를 발송한다.
                    - 인증번호 유효시간: 10분
                    - 발송 한도: 시간당 5회 / 하루 10회, 초과 시 일정 시간 잠금
                    - 이미 PENDING 상태의 코드가 있으면 새 코드 발송 시 기존 코드는 즉시 무효화된다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 (`GlobalErrorCode.VALIDATION_ERROR`)"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일 (`AuthErrorCode.DUPLICATE_EMAIL`)"),
            @ApiResponse(responseCode = "429", description = "발송 횟수 한도 초과 또는 잠금 상태 (`AuthErrorCode.EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED`)"),
            @ApiResponse(responseCode = "502", description = "메일 발송 실패 (`GlobalErrorCode.EXTERNAL_API_ERROR`)"),
    })
    @PostMapping("/email/verification")
    public CommonResponse<Void> sendEmailVerificationCode(@Valid @RequestBody EmailVerificationSendRequest request) {
        emailVerificationCommandService.sendSignupVerificationCode(request.email());
        return CommonResponse.success(null);
    }
}