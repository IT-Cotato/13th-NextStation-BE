package com.cotato.nextstation.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

@Schema(description = "회원가입 비밀번호 설정 요청")
public record SignupRequest(

        @Schema(description = "이메일 인증이 완료된 이메일 주소", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @Schema(description = "비밀번호 (영문·숫자·특수기호 포함 8-20자)", example = "abc12345!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수기호를 포함해 8-20자로 입력해주세요."
        )
        String password,

        @Schema(description = "비밀번호 확인", example = "abc12345!")
        @NotBlank(message = "비밀번호 확인은 필수입니다.")
        String passwordConfirm,

        @Schema(description = "동의한 약관 ID 목록", example = "[1, 2]")
        @NotEmpty(message = "동의한 약관 목록은 필수입니다.")
        List<Long> agreedTermsIds
) {
}