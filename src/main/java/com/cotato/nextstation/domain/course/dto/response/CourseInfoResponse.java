package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "코스 조회 전용 응답으로, 코스 정보가 필요할 때 사용한다.")
public record CourseInfoResponse(

        @Schema(description = "코스 ID", example = "1")
        Long courseId,

        @Schema(description = "코스 이름", example = "보문역 코스")
        String name,

        @Schema(description = "코스 소유자 회원 ID", example = "1")
        Long memberId,

        @Schema(description = "역 ID", example = "6")
        Long stationId,

        @Schema(description = "여행일지 ID (일지 작성 전이면 null)", example = "null")
        Long journalId,

        @Schema(description = "조회수", example = "10")
        int viewCount,

        @Schema(description = "좋아요 수", example = "3")
        int likeCount,

        @Schema(description = "코스 생성 시각", example = "2026-07-06T12:30:00")
        LocalDateTime createdAt
) {
}
