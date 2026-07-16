package com.cotato.nextstation.domain.departure.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DepartureStationCreateRequest(

        @NotNull(message = "역 ID는 필수입니다.")
        Long stationId,

        @Size(max = 30, message = "라벨은 30자 이하여야 합니다.")
        String label
) {
}
