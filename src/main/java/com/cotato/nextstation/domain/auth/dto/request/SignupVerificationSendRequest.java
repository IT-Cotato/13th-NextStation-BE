package com.cotato.nextstation.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "회원가입 이메일 인증번호 발송 요청")
public record SignupVerificationSendRequest(

        @Schema(description = "인증번호를 받을 이메일 주소", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Schema(description = "동의한 약관 ID 목록", example = "[1, 2]")
        @NotEmpty(message = "동의한 약관 목록은 필수입니다.")
        List<Long> agreedTermsIds
) {
}
