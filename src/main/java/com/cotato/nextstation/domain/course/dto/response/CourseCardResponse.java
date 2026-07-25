package com.cotato.nextstation.domain.course.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

// 코스 목록 카드. 저장 탭의 "내가 만든 코스"와 "좋아요한 코스" 목록이 같은 모양이라 함께 쓴다.
// 좋아요 수/조회수는 이 화면에 없다(둘러보기·코스 상세에서 사용).
@Schema(description = "코스 목록 카드 (내가 만든 코스 / 좋아요한 코스 공용)")
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
