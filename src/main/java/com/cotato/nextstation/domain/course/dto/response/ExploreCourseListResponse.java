package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "둘러보기 코스 목록 (커서 페이징)")
public record ExploreCourseListResponse(

        @Schema(description = "코스 카드 목록")
        List<ExploreCourseResponse> courses,

        @Schema(description = "다음 페이지 커서. 그대로 cursor 파라미터에 넣어 요청한다. 마지막 페이지면 null",
                nullable = true)
        String nextCursor,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
}
