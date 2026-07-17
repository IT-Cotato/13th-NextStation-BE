package com.cotato.nextstation.domain.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseNameUpdateRequest(

        @NotBlank(message = "코스 이름은 필수입니다.")
        @Size(max = 20, message = "코스 이름은 최대 20자까지 입력할 수 있어요.")
        String name
) {
}
