package com.cotato.nextstation.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "장소 리뷰")
public record PlaceReviewResponse(
        @Schema(description = "리뷰 id", example = "501")
        Long reviewId,

        @Schema(description = "작성자 id", example = "1")
        Long writerId,

        @Schema(description = "작성자 닉네임", example = "여행하는 토끼")
        String writerNickname,

        @Schema(description = "작성자 프로필 이미지 URL", example = "https://s3.../profile/1.jpg")
        String writerProfileImageUrl,

        @Schema(description = "리뷰 내용", example = "골목이 조용해서 사진 찍기 좋았어요.")
        String content,

        @Schema(description = "리뷰 이미지 URL 목록")
        List<String> imageUrls,

        @Schema(description = "좋아요 개수", example = "11")
        long likeCount,

        @Schema(description = "현재 사용자의 좋아요 여부 (비로그인 시 항상 false)", example = "true")
        boolean isLiked,

        @Schema(description = "작성일시", example = "2026-07-08T10:00:00")
        LocalDateTime createdAt
) {
}