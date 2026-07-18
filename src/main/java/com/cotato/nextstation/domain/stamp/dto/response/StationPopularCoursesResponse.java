package com.cotato.nextstation.domain.stamp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "역별 인기 코스 목록 조회 응답")
public record StationPopularCoursesResponse(
        @Schema(description = "코스 목록")
        List<PopularCourseResponse> courses,

        @Schema(description = "다음 페이지 커서")
        String nextCursor,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
}