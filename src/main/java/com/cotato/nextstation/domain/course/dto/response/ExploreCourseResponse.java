package com.cotato.nextstation.domain.course.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// 둘러보기 화면의 코스 카드. 노선따라·검색·인기·컨셉 목록이 모두 이 모양을 쓴다.
@Schema(description = "둘러보기 코스 카드")
public record ExploreCourseResponse(

        @Schema(description = "코스 ID", example = "1")
        Long courseId,

        @Schema(description = """
                여행일지 ID. 카드를 누르면 이 id로 여행일지 상세를 연다.
                둘러보기는 공개된 여행일지가 있는 코스만 노출하므로 항상 값이 있다.
                """, example = "10")
        Long journalId,

        @Schema(description = """
                카드에 표시할 이름. 코스 이름(course.name)이 아니라 작성자가 지은 여행일지 제목
                (journal.title)이다(2026-08-12 변경). 공개 코스만 노출하므로 항상 값이 있다.
                """, example = "민성이랑 떠나는 신림 느좋투어")
        String name,

        @Schema(description = "코스가 속한 역 ID", example = "123")
        Long stationId,

        @Schema(description = "역 이름. 카드 배지에 노선과 함께 표시한다", example = "신림역")
        String stationName,

        @Schema(description = "카드 배지에 표시할 역의 대표 호선. 대표 호선이 없는 역이면 null", nullable = true)
        LineSummaryResponse line,

        @Schema(description = "코스 대표 태그 (최대 2개). 코스에 담긴 장소들의 태그를 집계한 상위 태그다.",
                example = "[\"자연과함께\", \"카페투어\"]")
        List<String> tags,

        @Schema(description = "좋아요 수", example = "128")
        int likeCount,

        @Schema(description = "요청한 회원이 이 코스에 좋아요를 눌렀는지. 비로그인이면 항상 false", example = "false")
        boolean isLiked,

        @Schema(description = """
                카드 배경 이미지. 작성자가 여행일지에 올린 첫 번째 사진이다.
                아직 사진 데이터가 없어 현재는 null로 내려간다.
                """, nullable = true)
        String imageUrl
) {
}
