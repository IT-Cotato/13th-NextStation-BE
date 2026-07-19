package com.cotato.nextstation.domain.station.init;

import com.cotato.nextstation.domain.station.entity.Line;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.repository.LineRepository;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ApplicationRunner에서 파일 IO(CSV 파싱)와 트랜잭션 범위를 분리하기 위해
 * DB 업데이트 로직만 별도 빈으로 분리했다.
 * 같은 클래스 내 self-invocation으로는 @Transactional 프록시가 적용되지 않아 별도 빈으로 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class DrawableStationWriter {

    private final StationRepository stationRepository;
    private final LineRepository lineRepository;

    @Transactional
    public void write(List<DrawableStationRow> rows) {
        int updatedCount = 0;
        int notFoundCount = 0;

        for (DrawableStationRow row : rows) {
            Optional<Station> station = stationRepository.findByStationName(row.stationName());
            if (station.isEmpty()) {
                log.warn("뽑기 대상 역을 찾을 수 없습니다: stationName={}", row.stationName());
                notFoundCount++;
                continue;
            }

            Line drawLine = lineRepository.findByName(row.lineName())
                    .orElseThrow(() -> new IllegalStateException("뽑기 대표 노선을 찾을 수 없습니다: lineName=" + row.lineName()));

            station.get().assignAsDrawable(drawLine, row.todo());
            updatedCount++;
        }

        log.info("뽑기 대상 역 지정 완료: updatedCount={}, notFoundCount={}", updatedCount, notFoundCount);
    }
}