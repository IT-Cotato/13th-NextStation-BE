package com.cotato.nextstation.domain.departure.dto.response;

import java.time.LocalDateTime;

public record DepartureStationResponse(
        Long id,
        Long stationId,
        String label,
        int orderNum,
        LocalDateTime createdAt
) {
}
