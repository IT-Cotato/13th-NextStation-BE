package com.cotato.nextstation.domain.place.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 장소 리뷰 저장 요청 DTO.
 *
 * journal 도메인이 place 도메인 내부에 직접 의존하지 않도록,
 * PlaceReviewCommandService의 입력값을 별도 DTO로 정의한다.
 * journal 도메인은 이 DTO를 통해서만 place 도메인과 통신한다.
 * review 길이 검증은 이 DTO를 만들어 넘기는 쪽(JournalCreateRequest.PlaceReviewRequest)에서
 * 이미 하지만, 이 DTO도 같은 제약을 명시해 둔다.
 */
@Schema(description = "장소 리뷰 저장 요청")
public record PlaceReviewCreateRequest(
        @Schema(description = "장소 ID", example = "10")
        Long placeId,

        @Schema(description = "리뷰 내용 (없으면 null)", example = "커피가 맛있었어요.")
        @Size(max = 500, message = "장소 리뷰는 최대 500자까지 입력 가능합니다.")
        String review,

        @Schema(description = "리뷰 이미지 URL (없으면 null, 현재 기획상 1개만)")
        String imageUrl
) {}