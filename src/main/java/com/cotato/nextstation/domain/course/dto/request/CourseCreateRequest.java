package com.cotato.nextstation.domain.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CourseCreateRequest(

        @NotBlank(message = "코스 이름은 필수입니다.")
        @Size(max = 20, message = "코스 이름은 20자 이하여야 합니다.")
        String name,

        @NotNull(message = "역 ID는 필수입니다.")
        Long stationId,

        Long conceptTourId,

        @NotNull(message = "장소 목록은 필수입니다.")
        @Size(min = 3, max = 10, message = "장소는 3개 이상 10개 이하로 선택해야 합니다.")
        List<@NotNull Long> placeIds
) {
}
