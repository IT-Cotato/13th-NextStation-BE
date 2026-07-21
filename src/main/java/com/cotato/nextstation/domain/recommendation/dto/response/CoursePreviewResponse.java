package com.cotato.nextstation.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "코스 미리보기(저장되지 않은 transient 코스)")
public record CoursePreviewResponse(
        @Schema(description = "기본 코스 이름(수정 가능)", example = "제기동역 환승여행 코스")
        String name,

        @Schema(description = "카테고리별 장소 목록(문화공간/식당/카페/산책 순, 없는 카테고리는 제외)")
        List<CoursePreviewPlaceResponse> places
) {
}
