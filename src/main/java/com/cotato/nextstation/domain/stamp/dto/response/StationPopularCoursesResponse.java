package com.cotato.nextstation.domain.stamp.dto.response;
import com.cotato.nextstation.domain.course.dto.response.PopularCourseResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "역별 인기 코스 목록 조회 응답")
public record StationPopularCoursesResponse(
        @Schema(description = "역 이름 (Station 도메인 미구현으로 현재 null)", example = "null")
        String stationName,

        @Schema(description = "호선 (Station 도메인 미구현으로 현재 null)", example = "null")
        String line,

        @Schema(description = "인기 코스 목록 (최대 3개)")
        List<PopularCourseResponse> courses
) {
}