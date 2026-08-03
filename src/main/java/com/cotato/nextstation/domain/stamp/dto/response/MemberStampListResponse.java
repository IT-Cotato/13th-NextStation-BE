package com.cotato.nextstation.domain.stamp.dto.response;

import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "다른 회원 스탬프 목록 조회 응답")
public record MemberStampListResponse(

        @Schema(description = "스탬프(방문한 역) 개수", example = "12")
        int stampCount,

        @Schema(description = "모은 스탬프 목록 (최근 방문순). 같은 역에서 여러 코스를 완료해도 역당 1개만 담긴다.")
        List<StationSummaryResponse> stamps
) {
}
