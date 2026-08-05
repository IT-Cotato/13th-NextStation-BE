package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "다른 회원 공개 코스 목록 조회 응답")
public record MemberCourseListResponse(

        @Schema(description = "공개 코스 목록 (최신순)")
        List<CourseCardResponse> courses,

        @Schema(description = "다음 페이지 커서 (없으면 마지막 페이지)")
        String nextCursor,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {
}
