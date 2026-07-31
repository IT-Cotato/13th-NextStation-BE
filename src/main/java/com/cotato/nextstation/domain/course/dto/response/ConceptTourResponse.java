package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "컨셉별 투어 카드")
public record ConceptTourResponse(

        @Schema(description = "컨셉투어 ID. 누르면 이 id로 컨셉별 코스 목록을 조회한다", example = "1")
        Long conceptTourId,

        @Schema(description = "컨셉 이름", example = "문구 투어")
        String name,

        @Schema(description = "컨셉 설명", example = "작은 문구점과 책방을 따라 걷는 아기자기한 환승여행 코스")
        String description,

        @Schema(description = """
                이 컨셉에 속한 코스 수. 목록에 실제로 보이는 것과 같도록 공개된 코스만 센다.
                """, example = "18")
        long courseCount
) {
}
