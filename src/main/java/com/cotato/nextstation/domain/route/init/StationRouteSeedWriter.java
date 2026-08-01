package com.cotato.nextstation.domain.route.init;

import com.cotato.nextstation.domain.route.entity.StationRoute;
import com.cotato.nextstation.domain.route.repository.StationRouteRepository;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ApplicationRunner에서 파일 IO와 트랜잭션 범위를 분리하기 위해 DB 저장 로직만 별도 빈으로 분리했다.
 * 역 이름을 id로 바꿔야 하므로 station 전체를 한 번만 조회해 캐시로 사용한다(행마다 조회하면 15,000번 쿼리).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class StationRouteSeedWriter {

    private static final int BATCH_SIZE = 1000;

    private final StationRepository stationRepository;
    private final StationRouteRepository stationRouteRepository;

    @Transactional
    public void write(List<StationRouteSeedRow> rows) {
        Map<String, Long> stationIdByName = stationRepository.findAll().stream()
                .collect(Collectors.toMap(Station::getStationName, Station::getId, (first, second) -> first));

        List<StationRoute> buffer = new ArrayList<>(BATCH_SIZE);
        int savedCount = 0;
        Map<String, Integer> unknownStationNames = new HashMap<>();

        for (StationRouteSeedRow row : rows) {
            Long departureStationId = stationIdByName.get(row.departureStationName());
            Long arrivalStationId = stationIdByName.get(row.arrivalStationName());

            if (departureStationId == null || arrivalStationId == null) {
                String missing = (departureStationId == null) ? row.departureStationName() : row.arrivalStationName();
                unknownStationNames.merge(missing, 1, Integer::sum);
                continue;
            }

            buffer.add(StationRoute.builder()
                    .departureStationId(departureStationId)
                    .arrivalStationId(arrivalStationId)
                    .durationMinutes(row.durationMinutes())
                    .distanceMeters(row.distanceMeters())
                    .transferCount(row.transferCount())
                    .build());

            if (buffer.size() >= BATCH_SIZE) {
                savedCount += flush(buffer);
            }
        }
        savedCount += flush(buffer);

        if (!unknownStationNames.isEmpty()) {
            log.warn("station 테이블에서 찾지 못한 역이 있어 해당 구간을 건너뜁니다: {}", unknownStationNames);
        }
        log.info("station_route 시딩 완료: savedCount={}", savedCount);
    }

    private int flush(List<StationRoute> buffer) {
        if (buffer.isEmpty()) {
            return 0;
        }
        int size = buffer.size();
        stationRouteRepository.saveAll(buffer);
        buffer.clear();
        return size;
    }
}
