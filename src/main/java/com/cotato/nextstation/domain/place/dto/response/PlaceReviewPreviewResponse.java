package com.cotato.nextstation.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "장소 리뷰 미리보기")
public record PlaceReviewPreviewResponse(
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

        @Schema(description = "리뷰 이미지 URL")
        String imageUrl,

        @Schema(description = "작성일시", example = "2026-07-06T10:00:00")
        LocalDateTime createdAt
) {
}