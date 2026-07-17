package com.cotato.nextstation.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "장소 상세 조회 응답")
public record PlaceDetailResponse(
        Long placeId,
        String placeName,
        String description,
        String category,
        String address,
        String contactNumber,

        List<String> images,
        List<String> tags,
        List<PlaceReviewPreviewResponse> reviews
) {
}