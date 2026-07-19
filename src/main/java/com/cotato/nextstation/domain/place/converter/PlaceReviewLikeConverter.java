package com.cotato.nextstation.domain.place.converter;

import com.cotato.nextstation.domain.place.dto.response.PlaceReviewLikeResponse;
import org.springframework.stereotype.Component;

@Component
public class PlaceReviewLikeConverter {

    public PlaceReviewLikeResponse toLikeResponse(Long reviewId, long likeCount, boolean isLiked) {
        return new PlaceReviewLikeResponse(reviewId, likeCount, isLiked);
    }
}