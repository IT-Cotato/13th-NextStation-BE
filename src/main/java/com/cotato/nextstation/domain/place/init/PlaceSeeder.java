package com.cotato.nextstation.domain.place.init;

import com.cotato.nextstation.domain.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 구글 시트("장소 데이터")를 카카오맵 API로 보강해 정제한 resources/data/places.csv를 읽어
 * Place/PlaceTagMapping을 최초 1회 시딩한다.
 * Station을 자연키(역명)로, Category/PlaceTag를 자연키(코드/이름)로 참조하므로
 * StationDataSeeder와 data.sql(Category/PlaceTag 마스터)이 먼저 실행되어 있어야 한다.
 * Place 테이블에 데이터가 이미 있으면 스킵하므로 재기동해도 중복 적재되지 않는다.
 * 파일 IO는 트랜잭션 밖에서 수행하고, DB 저장만 {@link PlaceSeedWriter}의 트랜잭션으로 묶는다.
 */
@Slf4j
@Component
@Profile("!prod")
@Order(3)
@RequiredArgsConstructor
public class PlaceSeeder implements ApplicationRunner {

    private static final String SEED_CSV_PATH = "data/places.csv";
    private static final String HASHTAG_DELIMITER = " ";

    private static final String[] CSV_HEADERS = {
            "포함 여부", "노선", "역명", "카테고리", "장소명", "해시태그", "설명",
            "주소", "전화번호", "x좌표", "y좌표", "kakao_place_url"
    };

    private final PlaceRepository placeRepository;
    private final PlaceSeedWriter placeSeedWriter;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (placeRepository.count() > 0) {
            log.warn("place 데이터가 이미 존재해 시딩을 건너뜁니다. count={}", placeRepository.count());
            return;
        }

        log.info("place 시딩 시작: source={}", SEED_CSV_PATH);

        List<PlaceSeedRow> rows = readSeedRows();
        placeSeedWriter.write(rows);
    }

    private List<PlaceSeedRow> readSeedRows() throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(CSV_HEADERS)
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        List<PlaceSeedRow> rows = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(new ClassPathResource(SEED_CSV_PATH).getInputStream(), StandardCharsets.UTF_8),
                format)) {
            for (CSVRecord record : parser) {
                String placeName = record.get("장소명").trim();
                if (placeName.isBlank()) {
                    continue;
                }

                List<String> hashtagTexts = Arrays.stream(record.get("해시태그").trim().split(HASHTAG_DELIMITER))
                        .filter(tag -> !tag.isBlank())
                        .toList();

                rows.add(new PlaceSeedRow(
                        record.get("역명").trim(),
                        record.get("카테고리").trim(),
                        placeName,
                        hashtagTexts,
                        record.get("설명").trim(),
                        record.get("주소").trim(),
                        blankToNull(record.get("전화번호")),
                        Double.valueOf(record.get("x좌표").trim()),
                        Double.valueOf(record.get("y좌표").trim()),
                        blankToNull(record.get("kakao_place_url"))
                ));
            }
        }
        return rows;
    }

    private String blankToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}