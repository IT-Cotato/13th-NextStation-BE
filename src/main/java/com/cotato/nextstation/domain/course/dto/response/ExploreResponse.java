package com.cotato.nextstation.domain.course.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "둘러보기 메인 화면 응답. 화면에 필요한 세 섹션을 한 번에 담는다")
public record ExploreResponse(

        @Schema(description = "사람들이 많이 찾는 코스 (좋아요 수 순, 최대 6개). 더보기는 `GET /api/v1/courses/popular`")
        List<ExploreCourseResponse> popularCourses,

        @Schema(description = "컨셉별 투어 (표시 순서대로 최대 3개). 더보기는 `GET /api/v1/concept-tours`")
        List<ConceptTourResponse> conceptTours,

        @Schema(description = """
                노선 칩 목록. 공개 코스가 하나라도 있는 노선만 담는다.
                칩을 바꾸면 `GET /api/v1/courses?lineId=`으로 다시 조회한다.
                """)
        List<LineSummaryResponse> lines,

        @Schema(description = """
                처음에 선택해 둘 노선. 아래 `lineCourses`가 이 노선의 코스다.
                노선이 하나도 없으면 null이다.
                """, example = "4", nullable = true)
        Long selectedLineId,

        @Schema(description = "선택된 노선의 코스 (최신순, 최대 3개)")
        List<ExploreCourseResponse> lineCourses
) {
}
