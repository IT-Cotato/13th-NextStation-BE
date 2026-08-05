package com.cotato.nextstation.domain.stamp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "다른 회원 스탬프 목록 조회 응답")
public record MemberStampListResponse(

        @Schema(description = "스탬프(방문한 역) 개수", example = "12")
        int stampCount,

        @Schema(description = "모은 스탬프 목록. 같은 역에서 여러 코스를 완료해도 역당 1개만 담긴다. " +
                "1호선 → 9호선 순으로 정렬되며, 대표 호선이 없는 역은 맨 뒤에 온다. 동일 호선 내에서는 역명 가나다순으로 정렬된다.")
        List<MemberStampResponse> stamps
) {
}
