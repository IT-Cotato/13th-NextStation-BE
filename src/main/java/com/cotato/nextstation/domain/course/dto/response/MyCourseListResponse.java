package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내가 만든 코스 목록 조회 응답")
public record MyCourseListResponse(

        @Schema(description = """
                내 코스가 하나라도 있는 호선 목록. 코스가 없는 호선 칩을 비활성화하는 데 쓴다.
                현재 필터와 무관하게 항상 전체 기준으로 내려주며, 최초 조회(cursor 없음) 시에만 채워진다.
                """)
        List<LineFilterResponse> availableLines,

        @Schema(description = "내가 만든 코스 목록 (최신순)")
        List<CourseCardResponse> courses,

        @Schema(description = "다음 페이지 커서 (없으면 마지막 페이지)")
        String nextCursor,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
}
