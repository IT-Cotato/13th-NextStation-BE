package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 수정 응답")
public record CourseUpdateResponse(

        @Schema(description = "코스 ID", example = "1")
        Long courseId,

        @Schema(description = "코스 이름 (수정 여부와 무관하게 현재 이름을 반환한다)", example = "나만의 보문역 코스")
        String name
) {
}
