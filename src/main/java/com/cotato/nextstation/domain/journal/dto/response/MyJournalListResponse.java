package com.cotato.nextstation.domain.journal.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내 여행일지 목록 응답")
public record MyJournalListResponse(

        @Schema(description = "여행일지 목록 (최신순)")
        List<MyJournalResponse> journals
) {
}
