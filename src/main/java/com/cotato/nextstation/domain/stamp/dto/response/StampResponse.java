package com.cotato.nextstation.domain.stamp.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스탬프 단건 정보 (역 기준, 역별로 최초 획득 스탬프 하나만 노출)")
public record StampResponse(

        @Schema(description = "역 ID", example = "5")
        Long stationId,

        @Schema(description = "역명", example = "제기동역")
        String stationName,

        @Schema(description = "소속 노선. 뽑기 대상이 아닌 역이면 null일 수 있다.")
        LineSummaryResponse line
) {
}
