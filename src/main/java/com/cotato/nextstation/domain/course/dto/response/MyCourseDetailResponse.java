package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내가 만든 코스 확인 화면 응답 (지도 + 코스 순서)")
public record MyCourseDetailResponse(

        @Schema(description = "코스 ID", example = "1")
        Long courseId,

        @Schema(description = "코스 이름", example = "민성이랑 떠나는 느좋투어")
        String name,

        @Schema(description = "코스가 속한 역 ID", example = "6")
        Long stationId,

        @Schema(description = "역 이름", example = "신림역")
        String stationName,

        @Schema(description = "코스에 담긴 장소 (순서대로)")
        List<MyCoursePlaceResponse> places
) {
}
