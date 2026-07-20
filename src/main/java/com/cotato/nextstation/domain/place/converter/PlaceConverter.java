package com.cotato.nextstation.domain.place.converter;

import com.cotato.nextstation.domain.place.dto.response.PlaceDetailResponse;
import com.cotato.nextstation.domain.place.dto.response.PlaceReviewPreviewResponse;
import com.cotato.nextstation.domain.place.entity.Place;
import com.cotato.nextstation.domain.place.entity.PlaceImage;
import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.entity.PlaceReviewImage;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PlaceConverter {


    public PlaceDetailResponse toDetailResponse(
            Place place,
            List<PlaceImage> placeImages,
            List<PlaceReview> reviews,
            List<PlaceReviewImage> reviewImages
    ) {

        Map<Long, List<String>> imagesByReviewId = reviewImages.stream()
                .collect(Collectors.groupingBy(
                        image -> image.getPlaceReview().getId(),
                        Collectors.mapping(PlaceReviewImage::getImageUrl, Collectors.toList())
                ));

        return new PlaceDetailResponse(
                place.getId(),
                place.getPlaceName(),
                place.getDescription(),
                place.getCategory().getName(),
                place.getAddress(),
                place.getContactNumber(),
                place.getKakaoPlaceUrl(),
                toImageUrls(place, placeImages),
                toReviewPreviews(reviews, imagesByReviewId)
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

    private List<PlaceReviewPreviewResponse> toReviewPreviews(
            List<PlaceReview> reviews,
                    Map<Long, List<String >> imagesByReviewId
    ){
        return reviews.stream()
                .map(review -> toReviewPreview(review, imagesByReviewId))
                .toList();
    }

    private PlaceReviewPreviewResponse toReviewPreview(PlaceReview review, Map<Long, List<String>> imagesByReviewId) {
        return new PlaceReviewPreviewResponse(
                review.getId(),
                review.getJournal().getMember().getId(),
                review.getJournal().getMember().getNickname(),
                review.getJournal().getMember().getProfileImageUrl(),
                review.getReview(),
                imagesByReviewId.getOrDefault(review.getId(), List.of()),
                review.getCreatedAt()
        );
    }
}