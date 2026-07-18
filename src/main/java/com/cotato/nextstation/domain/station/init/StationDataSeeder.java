package com.cotato.nextstation.domain.station.init;

import com.cotato.nextstation.domain.station.entity.Line;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.entity.StationLine;
import com.cotato.nextstation.domain.station.repository.LineRepository;
import com.cotato.nextstation.domain.station.repository.StationLineRepository;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 공공데이터포털 "전국도시철도역사정보표준데이터"를 서울 기준으로 정제한
 * resources/data/seoul_stations.csv 를 읽어 Station/Line/StationLine을 최초 1회 시딩한다.
 * Station 테이블에 데이터가 이미 있으면 스킵하므로 재기동해도 중복 적재되지 않는다.
 */
@Slf4j
@Component
@Profile("!prod")
@Order(1)
@RequiredArgsConstructor
public class StationDataSeeder implements ApplicationRunner {

    private static final String SEED_CSV_PATH = "data/seoul_stations.csv";
    private static final String CSV_DELIMITER = ",";
    private static final String LINE_NAME_DELIMITER = "\\|";

    private final StationRepository stationRepository;
    private final LineRepository lineRepository;
    private final StationLineRepository stationLineRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (stationRepository.count() > 0) {
            log.info("station 데이터가 이미 존재해 시딩을 건너뜁니다. count={}", stationRepository.count());
            return;
        }

        log.info("station 시딩 시작: source={}", SEED_CSV_PATH);

        Map<String, Line> lineCache = new HashMap<>();
        int stationCount = 0;
        int stationLineCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(SEED_CSV_PATH).getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // header

            String row;
            while ((row = reader.readLine()) != null) {
                if (row.isBlank()) {
                    continue;
                }

                String[] columns = row.split(CSV_DELIMITER, 2);
                String stationName = columns[0].trim();
                String[] lineNames = columns[1].trim().split(LINE_NAME_DELIMITER);

                Station station = stationRepository.save(
                        Station.builder()
                                .stationName(stationName)
                                .isDrawable(false)
                                .build()
                );
                stationCount++;

                for (String lineName : lineNames) {
                    Line line = lineCache.computeIfAbsent(lineName,
                            name -> lineRepository.findByName(name).orElseGet(() -> lineRepository.save(Line.of(name))));
                    stationLineRepository.save(StationLine.of(station, line));
                    stationLineCount++;
                }
            }
        }

        log.info("station 시딩 완료: stationCount={}, lineCount={}, stationLineCount={}",
                stationCount, lineCache.size(), stationLineCount);
    }
}