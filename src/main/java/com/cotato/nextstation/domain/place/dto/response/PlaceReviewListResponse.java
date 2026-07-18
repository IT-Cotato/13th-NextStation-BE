package com.cotato.nextstation.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "장소 리뷰 목록 조회 응답")
public record PlaceReviewListResponse(
        @Schema(description = "리뷰 목록")
        List<PlaceReviewResponse> reviews,

        @Schema(description = "다음 페이지 커서 (없으면 마지막 페이지)")
        String nextCursor,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
}