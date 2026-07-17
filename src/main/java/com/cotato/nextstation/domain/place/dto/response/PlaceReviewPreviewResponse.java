package com.cotato.nextstation.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장소 리뷰 미리보기")
public record PlaceReviewPreviewResponse(
        Long reviewId,
        String writerNickname,
        String content
) {
}