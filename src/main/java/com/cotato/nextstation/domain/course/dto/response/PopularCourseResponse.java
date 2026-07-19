package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역별 인기 코스 (조회 전용 포트). 스탬프/둘러보기 등 다른 화면에서 인기순 코스가 필요할 때 사용한다.")
public record PopularCourseResponse(

        @Schema(description = "코스 ID", example = "1")
        Long courseId,

        @Schema(description = "코스 이름", example = "보문역 코스")
        String name,

        @Schema(description = "조회수", example = "300")
        int viewCount,

        @Schema(description = "저장 수", example = "128")
        int saveCount
) {
}
