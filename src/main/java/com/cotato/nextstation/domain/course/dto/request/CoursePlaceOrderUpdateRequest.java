package com.cotato.nextstation.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "코스 장소 순서 수정 요청")
public record CoursePlaceOrderUpdateRequest(

        @Schema(description = "재정렬할 장소 ID 목록. 코스의 기존 장소 구성과 일치해야 하며, 배열 순서대로 order_num이 부여된다.", example = "[3, 1, 4, 2]")
        @NotNull(message = "장소 목록은 필수입니다.")
        @Size(min = 3, max = 10, message = "장소는 3개 이상 10개 이하로 선택해야 합니다.")
        List<@NotNull Long> placeIds
) {
}
