package com.cotato.nextstation.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseNameUpdateRequest(

        @Schema(description = "새 코스 이름 (최대 20자)", example = "나만의 보문역 코스")
        @NotBlank(message = "코스 이름은 필수입니다.")
        @Size(max = 20, message = "코스 이름은 최대 20자까지 입력할 수 있어요.")
        String name
) {
}
