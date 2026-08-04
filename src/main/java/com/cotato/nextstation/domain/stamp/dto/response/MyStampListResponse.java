package com.cotato.nextstation.domain.stamp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내 스탬프 목록 응답")
public record MyStampListResponse(

        @Schema(description = "스탬프 총 개수 (역 기준 중복 제거 후)", example = "3")
        int totalCount,

        @Schema(description = "스탬프 목록. 1호선 → 9호선 순으로 정렬되며, line이 없는 역은 맨 뒤에 온다. 동일 호선 내에서는 역명 가나다순으로 정렬된다.")
        List<StampResponse> stamps
) {
}
