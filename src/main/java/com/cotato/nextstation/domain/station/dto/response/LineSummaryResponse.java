package com.cotato.nextstation.domain.station.dto.response;

import com.cotato.nextstation.domain.station.entity.LineCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "노선 요약 정보")
public record LineSummaryResponse(

        @Schema(description = "노선 ID", example = "1")
        Long id,

        @Schema(description = "노선명", example = "1호선")
        String name,

        @Schema(description = "노선 코드", example = "LINE_1")
        LineCode code
) {
}