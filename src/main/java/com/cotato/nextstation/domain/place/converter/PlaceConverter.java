package com.cotato.nextstation.domain.place.converter;

import com.cotato.nextstation.domain.place.dto.response.PlaceDetailResponse;
import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.dto.response.PlaceReviewPreviewResponse;
import com.cotato.nextstation.domain.place.entity.Place;
import com.cotato.nextstation.domain.place.entity.PlaceImage;
import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.entity.PlaceReviewImage;
import com.cotato.nextstation.domain.place.repository.PlaceImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PlaceConverter {

    private final PlaceImageRepository placeImageRepository;

    // ===== 장소 상세 조회(PlaceDetailResponse)용 =====

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
                toImageUrls(place, placeImages),
                toReviewPreviews(reviews, reviewImages)
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

    private List<PlaceReviewPreviewResponse> toReviewPreviews(List<PlaceReview> reviews, List<PlaceReviewImage> reviewImages) {
        Map<Long, List<String>> imagesByReviewId = reviewImages.stream()
                .collect(Collectors.groupingBy(
                        image -> image.getPlaceReview().getId(),
                        Collectors.mapping(PlaceReviewImage::getImageUrl, Collectors.toList())
                ));

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

    // ===== 조회 전용 포트(PlaceInfoResponse)용  =====

    public List<PlaceInfoResponse> toPlaceInfoResponses(List<Place> places) {
        if (places.isEmpty()) {
        return List.of();
        }
        List<Long> placeIds = places.stream().map(Place::getId).toList();
        Map<Long, String> imageUrlByPlaceId = resolveImageUrlsByPlaceId(placeIds, places);

        return places.stream()
                .map(place -> toPlaceInfoResponse(place, imageUrlByPlaceId.get(place.getId())))
                .toList();
    }

        private Map<Long, String> resolveImageUrlsByPlaceId(List<Long> placeIds, List<Place> places) {
        List<PlaceImage> allImages = placeImageRepository.findByPlaceIdIn(placeIds);
        Map<Long, List<PlaceImage>> imagesByPlaceId = allImages.stream()
                .collect(Collectors.groupingBy(image -> image.getPlace().getId()));

        Map<Long, String> result = new HashMap<>();
        for (Place place : places) {
            List<PlaceImage> images = imagesByPlaceId.getOrDefault(place.getId(), List.of());
            String imageUrl = images.isEmpty()
                    ? place.getCategory().getDefaultImageUrl()
                    : images.get(0).getImageUrl();
            result.put(place.getId(), imageUrl);
        }
        return result;
    }

    private PlaceInfoResponse toPlaceInfoResponse(Place place, String imageUrl) {
        return new PlaceInfoResponse(
                place.getId(),
                place.getPlaceName(),
                place.getDescription(),
                place.getCategory().getCode().name(),
                place.getCategory().getName(),
                imageUrl,
                place.getXCoordinate(),
                place.getYCoordinate()
        );
    }
}