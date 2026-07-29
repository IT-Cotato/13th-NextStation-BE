package com.cotato.nextstation.domain.place.dto.request;

import com.cotato.nextstation.domain.journal.enums.ImageAction;

public record PlaceReviewUpdateRequest(
        Long placeId,
        String review,
        ImageAction imageAction,  // null이면 KEEP으로 간주
        String imageUrl
) {}