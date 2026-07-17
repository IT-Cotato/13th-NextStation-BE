package com.cotato.nextstation.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장소 리뷰 좋아요 처리 결과")
public record PlaceReviewLikeResponse(
        @Schema(description = "리뷰 id", example = "501")
        Long reviewId,

        @Schema(description = "좋아요 개수", example = "13")
        long likeCount,

        @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
        boolean isLiked
) {
}