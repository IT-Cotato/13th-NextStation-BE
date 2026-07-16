package com.cotato.nextstation.domain.departure.converter;

import com.cotato.nextstation.domain.departure.dto.request.DepartureStationCreateRequest;
import com.cotato.nextstation.domain.departure.dto.response.DepartureStationResponse;
import com.cotato.nextstation.domain.departure.entity.MemberDepartureStation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DepartureStationConverter {

    public MemberDepartureStation toEntity(Long memberId, DepartureStationCreateRequest request, int orderNum) {
        return MemberDepartureStation.builder()
                .memberId(memberId)
                .stationId(request.stationId())
                .label(request.label())
                .orderNum(orderNum)
                .build();
    }

    public DepartureStationResponse toResponse(MemberDepartureStation departureStation) {
        return new DepartureStationResponse(
                departureStation.getId(),
                departureStation.getStationId(),
                departureStation.getLabel(),
                departureStation.getOrderNum(),
                departureStation.getCreatedAt()
        );
    }

    public List<DepartureStationResponse> toResponses(List<MemberDepartureStation> departureStations) {
        return departureStations.stream()
                .map(this::toResponse)
                .toList();
    }
}
