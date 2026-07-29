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

        @Schema(description = "여행일지 제목", example = "보문 골목 산책")
        @Size(max = 20, message = "제목은 최대 20자까지 입력 가능합니다.") // 코스 이름이 최대 20자 -> 통일
        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @Size(max = 1000, message = "여행 전체 후기는 최대 1000자까지 입력 가능합니다.")
        @Schema(description = "여행 전체 후기", example = "날씨가 좋아서 걷기 좋았다.",
        maxLength = 1000
        )
        String overallReview,

        @Schema(description = "여행 날짜", example = "2026-07-08")
        @NotNull(message = "방문 날짜는 필수입니다.")
        LocalDate traveledAt,

        @Schema(
                description = "여행 소요 시간", example = "HALF_DAY",
                allowableValues = {"SHORT", "HALF_DAY", "FULL_DAY"}
        )
        @NotNull(message = "코스 시간은 필수입니다.")
        TravelDuration travelDuration,

        @Schema(description = "공개 여부", example = "true")
        @NotNull(message = "공개 여부는 필수입니다.")
        Boolean isPublic,

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