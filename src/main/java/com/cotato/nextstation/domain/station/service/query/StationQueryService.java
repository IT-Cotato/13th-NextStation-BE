package com.cotato.nextstation.domain.station.service.query;

import com.cotato.nextstation.domain.station.converter.StationConverter;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.repository.StationLineRepository;
import com.cotato.nextstation.domain.station.repository.StationLineRepository.StationLineNameView;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 역 조회 전용 서비스. 다른 도메인이 역 정보가 필요할 때 이 서비스를 호출한다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StationQueryService {

    private final StationRepository stationRepository;
    private final StationLineRepository stationLineRepository;
    private final StationConverter stationConverter;

    // 역 이름 검색 (현재 전체일치). 못 찾으면 빈 목록
    public List<StationSummaryResponse> searchByName(String keyword) {
        return stationRepository.findByStationName(keyword)
                .map(station -> {
                    Map<Long, List<String>> lines = groupLineNames(List.of(station.getId()));
                    return List.of(stationConverter.toSummaryResponse(station, lines.getOrDefault(station.getId(), List.of())));
                })
                .orElseGet(List::of);
    }

    // 출발역 목록 등에서 stationId들로 역 요약을 일괄 조회할 때 사용
    public Map<Long, StationSummaryResponse> getSummariesByStationIds(Collection<Long> stationIds) {
        if (stationIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> linesByStation = groupLineNames(stationIds);
        return stationRepository.findAllById(stationIds).stream()
                .collect(Collectors.toMap(
                        Station::getId,
                        station -> stationConverter.toSummaryResponse(
                                station, linesByStation.getOrDefault(station.getId(), List.of()))
                ));
    }

    // 여러 역의 소속 노선명을 stationId 기준으로 묶는다.
    private Map<Long, List<String>> groupLineNames(Collection<Long> stationIds) {
        return stationLineRepository.findLineNamesByStationIdIn(stationIds).stream()
                .collect(Collectors.groupingBy(
                        StationLineNameView::getStationId,
                        Collectors.mapping(StationLineNameView::getLineName, Collectors.toList())
                ));
    }
}
