package com.cotato.nextstation.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장소 조회 전용 응답으로, 다른 도메인에서 장소 정보가 필요할 때 사용한다.")
public record PlaceInfoResponse(
        @Schema(description = "장소 ID", example = "1")
        Long placeId,

        @Schema(description = "장소명", example = "보문숲길도서관")
        String placeName,

        @Schema(description = "설명", example = "혼자 조용히 머물기 좋은 동네 도서관")
        String description,

        @Schema(description = "카테고리", example = "CULTURE")
        String categoryCode,

        @Schema(description = "카테고리명", example = "문화공간")
        String categoryName,

        @Schema(description = "대표 이미지 URL (장소 이미지 없으면 카테고리 기본 이미지로 대체)")
        String imageUrl,

        @Schema(description = "x좌표", example = "127.123")
        Double xCoordinate,

        @Schema(description = "y좌표", example = "37.456")
        Double yCoordinate
) {
}