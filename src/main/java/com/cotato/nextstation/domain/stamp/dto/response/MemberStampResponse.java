package com.cotato.nextstation.domain.stamp.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "다른 회원 스탬프 항목")
public record MemberStampResponse(

        @Schema(description = "역 ID", example = "6")
        Long stationId,

        @Schema(description = "역명", example = "보문역")
        String stationName,

        @Schema(description = "역의 대표 호선. 대표 호선이 없는 역이면 null", nullable = true)
        LineSummaryResponse line
) {
}
