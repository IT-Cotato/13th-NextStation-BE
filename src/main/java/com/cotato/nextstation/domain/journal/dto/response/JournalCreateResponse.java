package com.cotato.nextstation.domain.journal.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "여행일지 작성 응답")
public record JournalCreateResponse(
        @Schema(description = "여행일지 ID", example = "501")
        Long journalId
) {}