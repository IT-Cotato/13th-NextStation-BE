package com.cotato.nextstation.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "내가 만든 코스 다중 삭제 요청")
public record MyCourseDeleteRequest(

        @Schema(description = "삭제할 코스 ID 목록", example = "[1, 2, 3]")
        @NotEmpty(message = "삭제할 코스를 선택해주세요.")
        List<Long> courseIds
) {
}
