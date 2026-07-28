package com.cotato.nextstation.domain.recommendation.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "뽑힌 역 정보")
public record RecommendedStationResponse(
        @Schema(description = "역 ID", example = "12")
        Long stationId,

        @Schema(description = "역 이름", example = "보문역")
        String stationName,

        @Schema(description = "역 소개 문구", example = "성북천을 따라 천천히 걷고, 대학가와 오래된 주거 골목 사이의 조용한 생활감을 느낄 수 있는 역이에요.")
        String description,

        @Schema(description = "역에서 해볼 만한 일 목록(결과 화면의 '○○역에선!' 리스트). 번호 접두사는 서버가 떼고 내려준다",
                example = "[\"성북천을 따라 가볍게 산책하기\", \"보문동 골목과 생활 상권 둘러보기\"]")
        List<String> todos,

        @Schema(description = "소속 노선 목록(환승역이면 여러 개). 대표 노선이 맨 앞이고 나머지는 노선 ID 순이다")
        List<LineSummaryResponse> lines
) {
}
