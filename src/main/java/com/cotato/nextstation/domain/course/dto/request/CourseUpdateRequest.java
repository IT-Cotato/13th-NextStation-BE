package com.cotato.nextstation.domain.course.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "코스 수정 요청 (이름·장소 순서 중 하나 이상 필수)")
public record CourseUpdateRequest(

        @Schema(description = "새 코스 이름 (최대 20자). 생략하면 이름은 그대로 둔다.", example = "나만의 보문역 코스")
        @Size(max = 20, message = "코스 이름은 최대 20자까지 입력할 수 있어요.")
        String name,

        @Schema(description = "재정렬할 장소 ID 목록. 코스의 기존 장소 구성과 정확히 일치해야 하며, " +
                "배열 순서대로 order_num이 부여된다. 생략하면 순서는 그대로 둔다.", example = "[3, 1, 4, 2]")
        @Size(min = 3, max = 10, message = "장소는 3개 이상 10개 이하로 선택해야 합니다.")
        List<@NotNull Long> placeIds
) {
    @AssertTrue(message = "이름 또는 장소 순서 중 하나 이상을 입력해주세요.")
    public boolean isAnyFieldProvided() {
        return name != null || placeIds != null;
    }

    @AssertTrue(message = "코스 이름은 공백일 수 없습니다.")
    public boolean isNameNotBlank() {
        return name == null || !name.isBlank();
    }
}
