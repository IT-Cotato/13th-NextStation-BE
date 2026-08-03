package com.cotato.nextstation.domain.stamp.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "내 스탬프 상세 응답 (해당 역 최초 방문 스탬프 기준)")
public record MyStampDetailResponse(

        @Schema(description = "역 ID", example = "5")
        Long stationId,

        @Schema(description = "역명", example = "제기동역")
        String stationName,

        @Schema(description = "소속 노선. 뽑기 대상이 아닌 역이면 null일 수 있다.")
        LineSummaryResponse line,

        @Schema(description = "최초 방문(스탬프 획득) 시각", example = "2026-03-02T14:20:00")
        LocalDateTime acquiredAt,

        @Schema(description = "최초 방문 스탬프에 연결된 여행일지 ID. 아직 작성하지 않았거나 삭제된 경우 null.", example = "42")
        Long journalId
) {
}
