package com.cotato.nextstation.domain.journal.dto.response;

import com.cotato.nextstation.domain.journal.enums.TravelDuration;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "여행일지 상세 조회 응답")
public record JournalDetailResponse(
        @Schema(description = "작성자 닉네임")
        String writerName,

        @Schema(description = "작성자 프로필 이미지 URL")
        String writerProfileImageUrl,

        @Schema(description = "방문 날짜")
        LocalDate traveledAt,

        @Schema(description = "소속 노선 목록(환승역이면 여러 개). 대표 노선이 맨 앞이고 나머지는 노선 ID 순이다")
        LineSummaryResponse line,

        @Schema(description = "역 이름", example = "보문역")
        String stationName,

        @Schema(description = "코스 이름", example = "보문에 살어리랏다")
        String courseName,

        @Schema(description = "태그 상위 3개")
        List<String> tags,

        @Schema(description = "코스 시간")
        TravelDuration travelDuration,

        @Schema(description = "조회수")
        int viewCount,

        @Schema(description = "저장 수")
        int saveCount,

        @Schema(description = "여행 사진 URL 목록 (첫 번째가 대표 사진)")
        List<String> imageUrls,

        @Schema(description = "전체 후기")
        String overallReview,

        @Schema(description = "방문 장소 목록")
        List<VisitedPlaceResponse> visitedPlaces
) {
    @Schema(description = "방문 장소 응답")
    public record VisitedPlaceResponse(
            int orderNum,
            Long placeId,
            String placeName,
            String review,
            String imageUrl
    ) {}
}