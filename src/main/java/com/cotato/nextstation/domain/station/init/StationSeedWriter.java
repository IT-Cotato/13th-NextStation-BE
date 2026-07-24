package com.cotato.nextstation.domain.station.init;

import com.cotato.nextstation.domain.station.entity.Line;
import com.cotato.nextstation.domain.station.entity.LineCode;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.entity.StationLine;
import com.cotato.nextstation.domain.station.repository.LineRepository;
import com.cotato.nextstation.domain.station.repository.StationLineRepository;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ApplicationRunner에서 파일 IO(CSV 파싱)와 트랜잭션 범위를 분리하기 위해
 * DB 저장 로직만 별도 빈으로 분리했다.
 * 같은 클래스 내 self-invocation으로는 @Transactional 프록시가 적용되지 않아 별도 빈으로 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class StationSeedWriter {

    private final StationRepository stationRepository;
    private final LineRepository lineRepository;
    private final StationLineRepository stationLineRepository;

    @Transactional
    public void write(List<StationSeedRow> rows) {
        Map<String, Line> lineCache = seedAllLines();
        int stationCount = 0;
        int stationLineCount = 0;

        for (StationSeedRow row : rows) {
            Station station = stationRepository.save(
                    Station.builder()
                            .stationName(row.stationName())
                            .isDrawable(false)
                            .build()
            );
            stationCount++;

            for (String lineName : row.lineNames()) {
                Line line = lineCache.get(lineName);
                if (line == null) {
                    throw new IllegalStateException("정의되지 않은 노선입니다: lineName=" + lineName);
                }
                stationLineRepository.save(StationLine.of(station, line));
                stationLineCount++;
            }
        }

        log.info("station 시딩 완료: stationCount={}, lineCount={}, stationLineCount={}",
                stationCount, lineCache.size(), stationLineCount);
    }

    /**
     * Line insert 순서를 CSV(역명) 순서와 분리해 LineCode 선언 순서로 고정한다.
     * 1~9호선을 먼저 선언해 두어서 AUTO_INCREMENT id도 항상 1~9로 배정된다.
     */
    private Map<String, Line> seedAllLines() {
        Map<String, Line> lineCache = new LinkedHashMap<>();
        for (LineCode code : LineCode.values()) {
            Line line = lineRepository.findByCode(code)
                    .orElseGet(() -> lineRepository.save(Line.of(code)));
            lineCache.put(line.getName(), line);
        }
        return lineCache;
    }
}