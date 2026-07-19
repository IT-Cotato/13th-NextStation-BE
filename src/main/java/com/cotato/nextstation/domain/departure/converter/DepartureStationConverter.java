package com.cotato.nextstation.domain.departure.converter;

import com.cotato.nextstation.domain.departure.dto.request.DepartureStationCreateRequest;
import com.cotato.nextstation.domain.departure.dto.response.DepartureStationCreateResponse;
import com.cotato.nextstation.domain.departure.dto.response.DepartureStationResponse;
import com.cotato.nextstation.domain.departure.entity.MemberDepartureStation;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

    public DepartureStationCreateResponse toCreateResponse(MemberDepartureStation departureStation) {
        return new DepartureStationCreateResponse(
                departureStation.getId(),
                departureStation.getStationId(),
                departureStation.getLabel(),
                departureStation.getOrderNum(),
                departureStation.getCreatedAt()
        );
    }

    // 역 요약(이름/노선)을 합쳐 응답으로 변환한다. summary가 없으면(역 못 찾음) 이름 null, 노선 빈 목록.
    public DepartureStationResponse toResponse(MemberDepartureStation departureStation, StationSummaryResponse summary) {
        return new DepartureStationResponse(
                departureStation.getId(),
                departureStation.getStationId(),
                summary != null ? summary.stationName() : null,
                summary != null ? summary.lines() : List.of(),
                departureStation.getLabel(),
                departureStation.getOrderNum(),
                departureStation.getCreatedAt()
        );
    }

    public List<DepartureStationResponse> toResponses(
            List<MemberDepartureStation> departureStations,
            Map<Long, StationSummaryResponse> summariesByStationId) {
        return departureStations.stream()
                .map(departureStation -> toResponse(
                        departureStation, summariesByStationId.get(departureStation.getStationId())))
                .toList();
    }
}
