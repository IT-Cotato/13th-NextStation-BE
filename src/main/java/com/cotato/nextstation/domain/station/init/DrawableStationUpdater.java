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
 * resources/data/drawable_stations.csv에 지정된 뽑기 대상 50개 역을 is_drawable=true로 표시하고,
 * 뽑기 결과에 노출할 대표 노선(draw_line) 하나를 지정한다
 * 뽑기 대상 역이 이미 하나라도 존재하면 스킵하므로 최초 1회만 적용된다.
 * StationDataSeeder가 먼저 station/line 데이터를 적재한 뒤 실행되어야 한다.
 * 파일 IO는 트랜잭션 밖에서 수행하고, DB 업데이트만 {@link DrawableStationWriter}의 트랜잭션으로 묶는다.
 */
@Slf4j
@Component
@Profile("!prod")
@Order(2)
@RequiredArgsConstructor
public class DrawableStationUpdater implements ApplicationRunner {

    private static final String DRAWABLE_CSV_PATH = "data/drawable_stations.csv";
    private static final String CSV_DELIMITER = ",";

    private final StationRepository stationRepository;
    private final DrawableStationWriter drawableStationWriter;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (stationRepository.existsByIsDrawableTrue()) {
            log.warn("뽑기 대상 역이 이미 지정되어 있어 업데이트를 건너뜁니다.");
            return;
        }

        log.info("뽑기 대상 역 지정 시작: source={}", DRAWABLE_CSV_PATH);

        List<DrawableStationRow> rows = readDrawableRows();
        drawableStationWriter.write(rows);
    }

    private List<DrawableStationRow> readDrawableRows() throws IOException {
        List<DrawableStationRow> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(DRAWABLE_CSV_PATH).getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // header

            String row;
            while ((row = reader.readLine()) != null) {
                if (row.isBlank()) {
                    continue;
                }

                String[] columns = row.split(CSV_DELIMITER, 3);
                String stationName = columns[0].trim();
                String lineName = columns[1].trim();
                String todo = columns[2].trim();

                rows.add(new DrawableStationRow(stationName, lineName, todo));
            }
        }

        return rows;
    }
}