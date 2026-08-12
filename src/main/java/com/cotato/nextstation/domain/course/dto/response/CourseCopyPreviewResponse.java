package com.cotato.nextstation.domain.course.dto.response;

import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// 타인의 공개 코스를 내 코스로 가져오기 직전에 보여주는 화면.
// 코스 구성만 필요해 여행일지 상세(사진·후기·작성자·태그)와 달리 코스와 장소만 담는다.
@Schema(description = "내 코스로 만들기 화면 응답 (지도 + 코스 순서)")
public record CourseCopyPreviewResponse(

        @Schema(description = "원본 코스 ID. 저장할 때 POST /courses/{courseId}/copy 에 그대로 쓴다", example = "7")
        Long courseId,

        @Schema(description = """
                이름 입력칸의 초기값. 코스 이름(course.name)이 아니라 원작성자가 지은 여행일지 제목
                (journal.title)이다(2026-08-12 변경). 사용자가 고치지 않으면 이 값 그대로
                POST /courses/{courseId}/copy 의 name에 실어 보낸다.
                """, example = "민성이랑 떠나는 신림 느좋투어")
        String name,

        @Schema(description = "코스가 속한 역 ID", example = "123")
        Long stationId,

        @Schema(description = "역 이름. 화면 상단 Next Station 배지에 쓴다", example = "보문역")
        String stationName,

        @Schema(description = """
                역의 대표 호선. 배지 색·모양을 이 값으로 정한다.
                대표 호선이 없는 역이면 null이다.
                """, nullable = true)
        LineSummaryResponse line,

        @Schema(description = "코스에 담긴 장소 (순서대로). 순서만 바꿔 저장할 수 있다")
        List<CoursePlaceDetailResponse> places
) {
}
