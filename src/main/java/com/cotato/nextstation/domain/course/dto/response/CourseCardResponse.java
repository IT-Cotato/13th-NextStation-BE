package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// 저장 탭의 코스 카드. 스크랩한 코스와 내가 만든 코스가 같은 모양이라 함께 쓴다.
// 저장 수/조회수는 이 화면에 없다(둘러보기·코스 상세에서 사용).
@Schema(description = "저장 탭 코스 카드")
public record CourseCardResponse(

        @Schema(description = "코스 ID", example = "1")
        Long courseId,

        @Schema(description = "코스 이름", example = "보문역 환승여행 코스")
        String name,

        @Schema(description = "코스가 속한 역 ID", example = "6")
        Long stationId,

        @Schema(description = "역 이름", example = "보문역")
        String stationName,

        @Schema(description = "역의 대표 호선 ID", example = "6")
        Long lineId,

        @Schema(description = "역의 대표 호선 이름", example = "6호선")
        String lineName
) {
}
