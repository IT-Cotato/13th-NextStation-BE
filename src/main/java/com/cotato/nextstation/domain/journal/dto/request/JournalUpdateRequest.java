package com.cotato.nextstation.domain.journal.dto.request;

import com.cotato.nextstation.domain.journal.enums.TravelDuration;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "여행일지 수정 요청 (null 필드는 수정하지 않음)")
public record JournalUpdateRequest(
        String title,
        String overallReview,
        LocalDate traveledAt,
        TravelDuration travelDuration,
        Boolean isPublic,
        List<JournalPhotoUpdateRequest> journalPhotos,
        List<PlaceReviewUpdateRequest> placeReviews
) {
    @Schema(description = "여행일지 대표 사진 수정 요청")
    public record JournalPhotoUpdateRequest(
            Long photoId,           // KEEP/DELETE 시 필요, UPDATE 시 null
            ImageAction imageAction,
            String image,           // UPDATE 시 새 imageUrl, 나머지는 null
            Boolean isRepresentative
    ) {}

    @Schema(description = "장소 리뷰 수정 요청")
    public record PlaceReviewUpdateRequest(
            Long placeId,
            String review,
            ImageAction imageAction,
            String image            // UPDATE 시 새 imageUrl, 나머지는 null
    ) {}

    public enum ImageAction {
        KEEP, DELETE, UPDATE
    }
}