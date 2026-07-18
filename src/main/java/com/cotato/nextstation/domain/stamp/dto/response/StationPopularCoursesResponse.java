package com.cotato.nextstation.domain.stamp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "역별 인기 코스 목록 조회 응답")
public record StationPopularCoursesResponse(
        @Schema(description = "인기 코스 목록 (최대 3개)")
        List<PopularCourseResponse> courses
) {
}