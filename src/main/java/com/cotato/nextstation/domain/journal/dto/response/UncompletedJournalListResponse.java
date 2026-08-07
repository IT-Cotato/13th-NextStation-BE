package com.cotato.nextstation.domain.journal.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "여행일지 미작성 목록 응답")
public record UncompletedJournalListResponse(
        @Schema(description = "전체 개수", example = "6")
        int totalCount,

        List<UncompletedCourseResponse> courses
) {
    @Schema(description = "여행일지 미작성 코스")
    public record UncompletedCourseResponse(
            @Schema(description = "스탬프 ID", example = "501")
            Long memberStampId,

            @Schema(description = "역 이름", example = "공릉역")
            String stationName,

            @Schema(description = "역의 대표 호선 (없으면 null)")
            LineSummaryResponse line,

            @Schema(description = "코스 이름", example = "철길 따라 은주랑 같이 다녀온 여행")
            String courseName,

            @Schema(description = "코스 대표 태그 2개", example = "[\"#동네탐색\", \"#자연과함께\"]")
            List<String> tags,

            @Schema(description = "스탬프 획득 시각", example = "2026-06-01T10:00:00")
            LocalDateTime acquiredAt
    ) {}
}