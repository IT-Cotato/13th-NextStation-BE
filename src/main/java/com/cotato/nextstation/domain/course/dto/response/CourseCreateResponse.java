package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "코스 생성 응답")
public record CourseCreateResponse(

        @Schema(description = "생성된 코스 ID", example = "1")
        Long courseId,

        @Schema(description = "코스 이름", example = "보문역 코스")
        String name,

        @Schema(description = "코스 생성 시각", example = "2026-07-06T12:30:00")
        LocalDateTime createdAt
) {
}
