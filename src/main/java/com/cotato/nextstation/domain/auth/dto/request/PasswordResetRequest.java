package com.cotato.nextstation.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "비밀번호 재설정 요청")
public record PasswordResetRequest(

        @Schema(description = "인증번호를 받은 이메일 주소", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Schema(description = "이메일로 발송된 6자리 인증번호", example = "123456")
        @NotBlank(message = "인증번호는 필수입니다.")
        @Pattern(regexp = "\\d{6}", message = "인증번호는 6자리 숫자여야 합니다.")
        String code,

        @Schema(description = "새 비밀번호 (영문·숫자·특수기호 포함 8-20자)", example = "abc12345!")
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수기호를 포함해 8-20자로 입력해주세요."
        )
        String newPassword,

        @Schema(description = "새 비밀번호 확인", example = "abc12345!")
        @NotBlank(message = "새 비밀번호 확인은 필수입니다.")
        String newPasswordConfirm
) {
}