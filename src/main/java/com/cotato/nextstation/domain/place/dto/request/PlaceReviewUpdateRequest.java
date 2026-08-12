package com.cotato.nextstation.domain.place.dto.request;

import com.cotato.nextstation.domain.journal.enums.ImageAction;
import jakarta.validation.constraints.Size;

public record PlaceReviewUpdateRequest(
        Long placeId,
        @Size(max = 500, message = "장소 리뷰는 최대 500자까지 입력 가능합니다.")
        String review,
        ImageAction imageAction,  // null이면 KEEP으로 간주
        String imageUrl
) {}