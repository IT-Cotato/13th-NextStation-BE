package com.cotato.nextstation.domain.departure.service.query;

import com.cotato.nextstation.domain.departure.converter.DepartureStationConverter;
import com.cotato.nextstation.domain.departure.dto.response.DepartureStationResponse;
import com.cotato.nextstation.domain.departure.entity.MemberDepartureStation;
import com.cotato.nextstation.domain.departure.repository.MemberDepartureStationRepository;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.service.query.StationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartureStationQueryService {

    private final MemberDepartureStationRepository memberDepartureStationRepository;
    private final StationQueryService stationQueryService;
    private final DepartureStationConverter departureStationConverter;

    public List<DepartureStationResponse> getDepartureStations(Long memberId) {
        List<MemberDepartureStation> departureStations =
                memberDepartureStationRepository.findByMemberIdOrderByOrderNumAsc(memberId);

        List<Long> stationIds = departureStations.stream()
                .map(MemberDepartureStation::getStationId)
                .toList();
        Map<Long, StationSummaryResponse> summaries = stationQueryService.getSummariesByStationIds(stationIds);

        return departureStationConverter.toResponses(departureStations, summaries);
    }
}
