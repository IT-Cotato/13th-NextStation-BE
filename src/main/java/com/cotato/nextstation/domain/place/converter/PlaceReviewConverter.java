package com.cotato.nextstation.domain.place.converter;

import com.cotato.nextstation.domain.place.dto.response.PlaceReviewListResponse;
import com.cotato.nextstation.domain.place.dto.response.PlaceReviewResponse;
import com.cotato.nextstation.domain.place.entity.PlaceReview;
import com.cotato.nextstation.domain.place.entity.PlaceReviewImage;
import com.cotato.nextstation.domain.place.repository.PlaceReviewImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PlaceReviewConverter {

    private final PlaceReviewImageRepository placeReviewImageRepository;

    public Map<Long, List<String>> resolveImagesByReviewId(List<PlaceReview> reviews) {
        if (reviews.isEmpty()) {
            return Map.of();
        }
        List<Long> reviewIds = reviews.stream().map(PlaceReview::getId).toList();
        List<PlaceReviewImage> images = placeReviewImageRepository.findByPlaceReviewIdIn(reviewIds);
        return images.stream()
                .collect(Collectors.groupingBy(
                        image -> image.getPlaceReview().getId(),
                        Collectors.mapping(PlaceReviewImage::getImageUrl, Collectors.toList())
                ));
    }

    public PlaceReviewListResponse toListResponse(
            Long totalCount,
            List<PlaceReview> reviews,
            Map<Long, List<String>> imagesByReviewId,
            Set<Long> likedReviewIds,
            String nextCursor,
            boolean hasNext
    ) {
        List<PlaceReviewResponse> reviewResponses = reviews.stream()
                .map(review -> toReviewResponse(review, imagesByReviewId, likedReviewIds))
                .toList();
        return new PlaceReviewListResponse(totalCount, reviewResponses, nextCursor, hasNext);
    }

    private PlaceReviewResponse toReviewResponse(
            PlaceReview review,
            Map<Long, List<String>> imagesByReviewId,
            Set<Long> likedReviewIds
    ) {
        return new PlaceReviewResponse(
                review.getId(),
                review.getJournal().getMember().getId(),
                review.getJournal().getMember().getNickname(),
                review.getJournal().getMember().getProfileImageUrl(),
                review.getReview(),
                imagesByReviewId.getOrDefault(review.getId(), List.of()),
                review.getLikeCount(),
                likedReviewIds.contains(review.getId()),
                review.getCreatedAt()
        );
    }
}