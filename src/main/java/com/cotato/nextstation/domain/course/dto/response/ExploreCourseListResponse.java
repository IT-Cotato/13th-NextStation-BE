package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "둘러보기 코스 목록 (커서 페이징)")
public record ExploreCourseListResponse(

        @Schema(description = "코스 카드 목록")
        List<ExploreCourseResponse> courses,

        @Schema(description = """
                "역 선택" 드롭다운에 그릴 역 목록. 공개 코스가 있는 역만 담긴다.
                `lineId`를 준 경우 그 노선의 역만 담기며, 역·검색어 필터는 반영하지 않는다.
                드롭다운은 화면에 한 번만 그리므로 **첫 페이지에서만** 채워지고 다음 페이지부터는 빈 배열이다.
                """)
        List<ExploreStationResponse> availableStations,

        @Schema(description = "다음 페이지 커서. 그대로 cursor 파라미터에 넣어 요청한다. 마지막 페이지면 null",
                nullable = true)
        String nextCursor,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
}
