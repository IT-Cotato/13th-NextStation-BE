package com.cotato.nextstation.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// 전체 선택 후 일부만 해제한 경우를 위해 제외 목록을 받는다.
// 해제는 화면에 보이는 항목에서만 일어나므로 프론트가 그 id는 알고 있다.
@Schema(description = "코스 좋아요 전체 취소 요청")
public record CourseLikeCancelAllRequest(

        @Schema(description = "취소 대상에서 뺄 코스 ID 목록. 전체를 취소할 때는 생략한다.", example = "[3, 7]")
        List<Long> exceptCourseIds
) {
}
