package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// 저장 탭 호선 필터 칩. 이름만 주면 프론트가 필터 요청에 쓸 id를 알 수 없어 id도 함께 준다.
@Schema(description = "선택 가능한 호선 필터")
public record LineFilterResponse(

        @Schema(description = "호선 ID", example = "6")
        Long lineId,

        @Schema(description = "호선 이름", example = "6호선")
        String lineName
) {
}
