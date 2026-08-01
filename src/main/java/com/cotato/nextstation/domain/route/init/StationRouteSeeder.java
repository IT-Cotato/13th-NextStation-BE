package com.cotato.nextstation.domain.route.init;

import com.cotato.nextstation.domain.route.repository.StationRouteRepository;
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
 * resources/data/station_route.csv 를 읽어 출발역 → 뽑기 대상 역 구간 정보를 최초 1회 시딩한다.
 * 역을 이름으로 참조하므로 StationDataSeeder(Order 1) 이후에 실행되어야 한다.
 * 파일 IO는 트랜잭션 밖에서 수행하고, DB 저장만 {@link StationRouteSeedWriter}의 트랜잭션으로 묶는다.
 */
@Slf4j
@Component
@Profile("!prod")
@Order(2)
@RequiredArgsConstructor
public class StationRouteSeeder implements ApplicationRunner {

    private static final String SEED_CSV_PATH = "data/station_route.csv";
    private static final String CSV_DELIMITER = ",";
    private static final int COLUMN_COUNT = 5;
    // 엑셀에서 저장한 CSV라 헤더 앞에 BOM이 붙어 있을 수 있다.
    private static final char BOM = '﻿';

    private final StationRouteRepository stationRouteRepository;
    private final StationRouteSeedWriter stationRouteSeedWriter;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (stationRouteRepository.count() > 0) {
            log.warn("station_route 데이터가 이미 존재해 시딩을 건너뜁니다. count={}", stationRouteRepository.count());
            return;
        }

        log.info("station_route 시딩 시작: source={}", SEED_CSV_PATH);

        List<StationRouteSeedRow> rows = readSeedRows();
        stationRouteSeedWriter.write(rows);
    }

    private List<StationRouteSeedRow> readSeedRows() throws IOException {
        List<StationRouteSeedRow> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(SEED_CSV_PATH).getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // header

            String row;
            while ((row = reader.readLine()) != null) {
                if (row.isBlank()) {
                    continue;
                }

                String[] columns = stripBom(row).split(CSV_DELIMITER);
                if (columns.length < COLUMN_COUNT) {
                    log.warn("컬럼 수가 부족한 행을 건너뜁니다: row={}", row);
                    continue;
                }

                rows.add(new StationRouteSeedRow(
                        columns[0].trim(),
                        columns[1].trim(),
                        Integer.parseInt(columns[2].trim()),
                        Integer.parseInt(columns[3].trim()),
                        parseNullableInt(columns[4].trim())
                ));
            }
        }

        return rows;
    }

    private String stripBom(String row) {
        return (!row.isEmpty() && row.charAt(0) == BOM) ? row.substring(1) : row;
    }

    private Integer parseNullableInt(String value) {
        return value.isBlank() ? null : Integer.valueOf(value);
    }
}
