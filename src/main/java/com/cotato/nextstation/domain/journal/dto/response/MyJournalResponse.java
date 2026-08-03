package com.cotato.nextstation.domain.journal.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 여행일지 목록 카드 응답")
public record MyJournalResponse(

        @Schema(description = "여행일지 ID", example = "900")
        Long journalId,

        @Schema(description = "역의 대표 호선 (없으면 null)")
        LineSummaryResponse line,

        @Schema(description = "여행일지 제목", example = "은서랑 같이 보문역 여행")
        String title,

        @Schema(description = "역 이름", example = "보문역")
        String stationName,

        @Schema(description = "대표 사진 URL (없으면 null)")
        String thumbnailUrl,

        @Schema(description = "코스 좋아요 수", example = "54")
        int likeCount
) {
}
