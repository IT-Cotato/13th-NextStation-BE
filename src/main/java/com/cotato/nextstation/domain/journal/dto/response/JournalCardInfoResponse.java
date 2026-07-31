package com.cotato.nextstation.domain.journal.dto.response;

import com.cotato.nextstation.domain.journal.enums.TravelDuration;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "여행일지 조회 전용 응답으로, 여행일지 정보가 필요할 때 사용한다.")
public record JournalCardInfoResponse(

        @Schema(description = "여행일지 ID", example = "501")
        Long journalId,

        @Schema(description = "여행 대표 사진 URL (없으면 null)")
        String imageUrl,

        @Schema(description = "여행 소요 시간 (없으면 null)")
        TravelDuration travelDuration
) {}