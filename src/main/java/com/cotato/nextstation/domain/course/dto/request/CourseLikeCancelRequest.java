package com.cotato.nextstation.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "코스 좋아요 다중 취소 요청")
public record CourseLikeCancelRequest(

        @Schema(description = "취소할 코스 ID 목록", example = "[1, 2, 3]")
        @NotEmpty(message = "취소할 코스를 선택해주세요.")
        List<Long> courseIds
) {
}
