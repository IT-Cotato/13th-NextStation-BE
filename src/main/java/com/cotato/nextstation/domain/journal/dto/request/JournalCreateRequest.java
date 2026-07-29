package com.cotato.nextstation.domain.journal.dto.request;

import com.cotato.nextstation.domain.journal.enums.TravelDuration;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "여행일지 작성 요청")
public record JournalCreateRequest(
        @NotNull(message = "스탬프 ID는 필수입니다.")
        Long memberStampId,

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        String overallReview,

        @NotNull(message = "방문 날짜는 필수입니다.")
        LocalDate traveledAt,

        @NotNull(message = "코스 시간은 필수입니다.")
        TravelDuration travelDuration,

        @NotNull(message = "공개 여부는 필수입니다.")
        boolean isPublic,

        @Size(max = 3, message = "여행 대표 사진은 최대 3장입니다.")
        List<String> journalImageUrls,

        List<PlaceReviewRequest> placeReviews
) {
    @Schema(description = "장소 리뷰 요청")
    public record PlaceReviewRequest(
            @NotNull
            Long placeId,
            String review,
            String imageUrl
    ) {}
}