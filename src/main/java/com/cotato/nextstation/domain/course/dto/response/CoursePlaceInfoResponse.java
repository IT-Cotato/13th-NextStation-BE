package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 내 장소 조회 전용 응답")
public record CoursePlaceInfoResponse(

        @Schema(description = "장소 ID", example = "12")
        Long placeId,

        @Schema(description = "코스 내 순서", example = "1")
        int orderNum
) {
}
