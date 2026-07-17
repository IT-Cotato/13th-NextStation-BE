package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 이름 수정 응답")
public record CourseNameResponse(

        @Schema(description = "코스 ID", example = "1")
        Long courseId,

        @Schema(description = "수정된 코스 이름", example = "나만의 보문역 코스")
        String name
) {
}
