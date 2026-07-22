package com.cotato.nextstation.domain.station.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "카테고리별 장소 묶음")
public record StationPlaceCategoryResponse(

        @Schema(description = "카테고리 코드", example = "CULTURE", allowableValues = {"CULTURE", "FOOD", "CAFE", "WALK"})
        String categoryCode,

        @Schema(description = "카테고리 표시명", example = "문화공간")
        String categoryName,

        @Schema(description = "해당 카테고리의 장소 목록 (최대 3개)")
        List<StationPlaceResponse> places
) {
}
