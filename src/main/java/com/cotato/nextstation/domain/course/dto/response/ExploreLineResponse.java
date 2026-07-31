package com.cotato.nextstation.domain.course.dto.response;

import com.cotato.nextstation.domain.station.entity.LineCode;
import io.swagger.v3.oas.annotations.media.Schema;

// 둘러보기의 노선 칩. 코스가 없는 노선도 비활성 상태로 그리도록 hasCourses를 함께 준다.
// 노선 요약(id/name/code) 필드명은 카드의 line과 맞춰 둔다.
@Schema(description = "둘러보기 노선 칩")
public record ExploreLineResponse(

        @Schema(description = "노선 ID. 그대로 lineId 파라미터에 넣어 요청한다", example = "6")
        Long id,

        @Schema(description = "노선명", example = "6호선")
        String name,

        @Schema(description = "노선 코드", example = "LINE_6")
        LineCode code,

        @Schema(description = """
                공개 코스가 하나라도 있는지. false면 눌러도 결과가 없으므로 칩을 비활성으로 그린다.
                """, example = "true")
        boolean hasCourses
) {
}
