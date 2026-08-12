package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "역별 인기 코스 (조회 전용 포트). 스탬프/둘러보기 등 다른 화면에서 인기순 코스가 필요할 때 사용한다.")
public record PopularCourseResponse(

        @Schema(description = "코스 ID", example = "1")
        Long courseId,

        @Schema(description = """
                카드에 표시할 이름. 코스 이름(course.name)이 아니라 작성자가 지은 여행일지 제목
                (journal.title)이다(2026-08-12 변경). 공개 코스만 노출하므로 항상 값이 있다.
                """, example = "보문역에서 하루")
        String name,

        @Schema(description = "조회수", example = "300")
        int viewCount,

        @Schema(description = "좋아요 수", example = "128")
        int likeCount,

        @Schema(description = "현재 로그인 사용자가 이 코스를 좋아요했는지 여부. memberId 없이 조회하면 항상 false", example = "true")
        boolean isLiked
) {
}
