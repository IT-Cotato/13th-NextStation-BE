package com.cotato.nextstation.domain.auth.controller;

import com.cotato.nextstation.domain.auth.dto.request.EmailVerificationConfirmRequest;
import com.cotato.nextstation.domain.auth.dto.request.SignupVerificationSendRequest;
import com.cotato.nextstation.domain.auth.service.command.EmailVerificationCommandService;
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
                    - 인증번호 유효시간: 3분
                    - 발송 한도: 시간당 5회 / 하루 10회, 초과 시 일정 시간 잠금
                    - 이미 PENDING 상태의 코드가 있으면 새 코드 발송 시 기존 코드는 즉시 무효화된다.
                    - 필수 약관에 모두 동의해야 발송된다(`agreedTermsIds`에 현재 필수 약관 id가 전부 포함돼야 함).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 또는 필수 약관 미동의 (`GlobalErrorCode.VALIDATION_ERROR`, `TermsErrorCode.REQUIRED_TERMS_NOT_AGREED`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 약관 id (`TermsErrorCode.TERMS_NOT_FOUND`)"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일 (`AuthErrorCode.DUPLICATE_EMAIL`)"),
            @ApiResponse(responseCode = "429", description = "발송 횟수 한도 초과 또는 잠금 상태 (`AuthErrorCode.EMAIL_VERIFICATION_RATE_LIMIT_EXCEEDED`)"),
            @ApiResponse(responseCode = "502", description = "메일 발송 실패 (`GlobalErrorCode.EXTERNAL_API_ERROR`)"),
    })
    @PostMapping("/email/verification")
    public CommonResponse<Void> sendEmailVerificationCode(@Valid @RequestBody SignupVerificationSendRequest request) {
        emailVerificationCommandService.sendSignupVerificationCode(request.email(), request.agreedTermsIds());
        return CommonResponse.success(null);
    }

    @Operation(
            summary = "회원가입 이메일 인증번호 확인",
            description = """
                    발송된 6자리 인증번호가 맞는지 확인한다.
                    - 인증번호 불일치 시도는 최대 5회까지 허용되며, 초과 시 해당 인증번호는 실패 처리된다.
                    - 만료된 인증번호로 확인을 시도하면 즉시 실패 처리된다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패, 만료된 인증번호, 또는 인증번호 불일치 (`GlobalErrorCode.VALIDATION_ERROR`, `AuthErrorCode.EMAIL_VERIFICATION_EXPIRED`, `AuthErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH`)"),
            @ApiResponse(responseCode = "404", description = "유효한 인증번호 발송 내역 없음 (`AuthErrorCode.EMAIL_VERIFICATION_NOT_FOUND`)"),
            @ApiResponse(responseCode = "429", description = "인증번호 확인 시도 횟수 초과 (`AuthErrorCode.EMAIL_VERIFICATION_ATTEMPT_EXCEEDED`)"),
    })
    @PostMapping("/email/verification/confirm")
    public CommonResponse<Void> confirmEmailVerificationCode(@Valid @RequestBody EmailVerificationConfirmRequest request) {
        emailVerificationCommandService.verifySignupCode(request.email(), request.code());
        return CommonResponse.success(null);
    }
}