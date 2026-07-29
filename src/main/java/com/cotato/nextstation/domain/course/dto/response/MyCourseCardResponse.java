package com.cotato.nextstation.domain.course.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

// 저장 탭 "내가 만든 코스" 카드. 스탬프 모양이 여행 완료 여부에 따라 달라져 완료 상태를 함께 내린다.
// 좋아요한 코스는 남의 코스라 완료 개념이 없어 CourseCardResponse를 그대로 쓴다.
@Schema(description = "내가 만든 코스 카드")
public record MyCourseCardResponse(

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
        LineSummaryResponse line,

        @Schema(description = "여행 완료 여부. 완료한 코스는 스탬프가 채워진 모양으로 표시한다", example = "true")
        boolean isCompleted
) {
}
