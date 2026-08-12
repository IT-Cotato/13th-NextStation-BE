package com.cotato.nextstation.domain.course.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

// 다른 회원 공개 코스 목록 카드. 좋아요한 코스 카드(CourseCardResponse)와 화면이 달라 따로 둔다.
// 그쪽은 좋아요 수·사진을 의도적으로 빼는데(둘러보기·상세 전용), 이 화면은 코스 상세로 바로
// 이동해야 해서 journalId가 필요하고, 마이페이지와 같은 카드 디자인을 쓰려면 imageUrl·likeCount도 필요하다.
@Schema(description = "다른 회원 공개 코스 목록 카드")
public record MemberCourseCardResponse(

        @Schema(description = "코스 ID", example = "1")
        Long courseId,

        @Schema(description = """
                여행일지 ID. 카드를 누르면 이 id로 여행일지 상세를 연다.
                공개 코스만 노출하므로 항상 값이 있다.
                """, example = "10")
        Long journalId,

        @Schema(description = "카드에 표시할 이름. journal.title을 사용한다.", example = "민성이랑 떠나는 신림 느좋투어")
        String name,

        @Schema(description = "코스가 속한 역 ID", example = "6")
        Long stationId,

        @Schema(description = "역 이름", example = "보문역")
        String stationName,

        @Schema(description = "카드 배지에 표시할 역의 대표 호선. 대표 호선이 없는 역이면 null",
                nullable = true)
        LineSummaryResponse line,

        @Schema(description = """
                카드 배경 이미지. 작성자가 여행일지에 올린 첫 번째 사진이다.
                아직 사진이 없으면 null이다.
                """, nullable = true)
        String imageUrl,

        @Schema(description = "좋아요 수", example = "12")
        int likeCount
) {
}
