package com.cotato.nextstation.domain.course.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

// 저장 탭의 "좋아요한 코스" 목록 카드.
// 내가 만든 코스는 여행 완료 여부까지 필요해 MyCourseCardResponse를 따로 쓴다(완료는 본인 코스만 가능).
// 좋아요 수/조회수는 이 화면에 없다(둘러보기·코스 상세에서 사용).
@Schema(description = "좋아요한 코스 목록 카드")
public record CourseCardResponse(

        @Schema(description = "코스 ID", example = "1")
        Long courseId,

        @Schema(description = "코스 이름", example = "보문역 환승여행 코스")
        String name,

        @Schema(description = "코스가 속한 역 ID", example = "6")
        Long stationId,

        @Schema(description = "역 이름", example = "보문역")
        String stationName,

        @Schema(description = "카드 배지에 표시할 역의 대표 호선. 대표 호선이 없는 역이면 null",
                nullable = true)
        LineSummaryResponse line
) {
}
