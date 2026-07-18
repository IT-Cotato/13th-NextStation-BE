package com.cotato.nextstation.domain.stamp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인기 코스")
public record PopularCourseResponse(
        @Schema(description = "코스 id", example = "1")
        Long courseId,

        @Schema(description = "코스 이름", example = "보문역 코스")
        String name,

        @Schema(description = "조회수", example = "300")
        int viewCount,

        @Schema(description = "저장 수", example = "128")
        int saveCount
) {
}