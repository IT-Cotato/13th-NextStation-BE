package com.cotato.nextstation.domain.course.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

// 저장 탭의 "좋아요한 코스" 목록 카드.
// 내가 만든 코스는 여행 완료 여부까지 필요해 MyCourseCardResponse를 따로 쓴다(완료는 본인 코스만 가능).
// 좋아요 수/조회수는 이 화면에 없다(둘러보기·코스 상세에서 사용).
@Schema(description = "좋아요한 코스 목록 카드")
public record CourseCardResponse(

        @Schema(description = "코스 ID", example = "1")
        Long courseId,

        @Schema(description = """
                카드를 눌렀을 때 열 여행일지 ID.
                좋아요는 공개된 여행일지가 있는 코스에만 걸 수 있어 항상 값이 있다.
                """, example = "10")
        Long journalId,

        @Schema(description = """
                카드에 표시할 이름. 코스 이름(course.name)이 아니라 작성자가 지은 여행일지 제목(journal.title)이다
                (2026-08-12 변경). 좋아요는 공개 코스만 대상이라 항상 값이 있다.
                """, example = "민성이랑 떠나는 신림 느좋투어")
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
        String imageUrl
) {
}
