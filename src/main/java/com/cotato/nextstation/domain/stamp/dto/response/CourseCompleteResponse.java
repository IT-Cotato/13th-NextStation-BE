package com.cotato.nextstation.domain.stamp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "코스 완료 처리 응답")
public record CourseCompleteResponse(
        @Schema(description = "스탬프 ID", example = "501")
        Long memberStampId,

        @Schema(description = "역 ID", example = "12")
        Long stationId,

        @Schema(description = "역 이름", example = "보문역")
        String stationName,

        @Schema(description = "스탬프 획득 시각", example = "2026-07-07T18:30:00")
        LocalDateTime acquiredAt
) {
}