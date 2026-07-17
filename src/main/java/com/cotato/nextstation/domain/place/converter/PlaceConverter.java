package com.cotato.nextstation.domain.place.converter;

import com.cotato.nextstation.domain.place.dto.response.PlaceDetailResponse;
import com.cotato.nextstation.domain.place.dto.response.PlaceReviewPreviewResponse;
import com.cotato.nextstation.domain.place.entity.Place;
import com.cotato.nextstation.domain.place.entity.PlaceImage;
import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.entity.PlaceTagMapping;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PlaceConverter {

    public PlaceDetailResponse toDetailResponse(
            Place place,
            List<PlaceImage> placeImages,
            List<PlaceTagMapping> tagMappings,
            List<PlaceReview> reviews
    ) {
        return new PlaceDetailResponse(
                place.getId(),
                place.getPlaceName(),
                place.getDescription(),
                place.getCategory().getName(),
                place.getAddress(),
                place.getContactNumber(),
                toImageUrls(place, placeImages),
                tagMappings.stream()
                        .map(mapping -> mapping.getPlaceTag().getName().name())
                        .toList(),
                reviews.stream()
                        .map(this::toReviewPreview)
                        .toList()
        );
    }

    // 이미지가 없으면 카테고리 기본 이미지로 폴백
    private List<String> toImageUrls(Place place, List<PlaceImage> placeImages) {
        if (placeImages.isEmpty()) {
            String defaultImageUrl = place.getCategory().getDefaultImageUrl();
            return defaultImageUrl != null ? List.of(defaultImageUrl) : List.of();
        }
        return placeImages.stream()
                .map(PlaceImage::getImageUrl)
                .toList();
    }

    private PlaceReviewPreviewResponse toReviewPreview(PlaceReview review) {
        return new PlaceReviewPreviewResponse(
                review.getId(),
                review.getJournal().getMemberId().toString(), // TODO: memberId → nickname 조회 필요
                review.getReview()
        );
    }
}