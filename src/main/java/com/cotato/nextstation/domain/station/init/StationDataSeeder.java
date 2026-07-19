package com.cotato.nextstation.domain.station.init;

import com.cotato.nextstation.domain.station.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 공공데이터포털 "전국도시철도역사정보표준데이터"를 서울 기준으로 정제한
 * resources/data/seoul_stations.csv 를 읽어 Station/Line/StationLine을 최초 1회 시딩한다.
 * Station 테이블에 데이터가 이미 있으면 스킵하므로 재기동해도 중복 적재되지 않는다.
 * 파일 IO는 트랜잭션 밖에서 수행하고, DB 저장만 {@link StationSeedWriter}의 트랜잭션으로 묶는다.
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
    private final StationSeedWriter stationSeedWriter;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (stationRepository.count() > 0) {
            log.warn("station 데이터가 이미 존재해 시딩을 건너뜁니다. count={}", stationRepository.count());
            return;
        }

        log.info("station 시딩 시작: source={}", SEED_CSV_PATH);

        List<StationSeedRow> rows = readSeedRows();
        stationSeedWriter.write(rows);
    }

    private List<StationSeedRow> readSeedRows() throws IOException {
        List<StationSeedRow> rows = new ArrayList<>();

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
                List<String> lineNames = List.of(columns[1].trim().split(LINE_NAME_DELIMITER));

                rows.add(new StationSeedRow(stationName, lineNames));
            }
        }

        return rows;
    }
}