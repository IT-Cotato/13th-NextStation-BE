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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

/**
 * 구글 시트("장소 데이터")를 카카오맵 API로 보강해 정제한 resources/data/places.csv를 읽어 Place/PlaceTagMapping을 시딩한다.
 * places.csv 내용의 해시를 {@link #SEED_HASH_MARKER}에 저장해두고, 이전 해시와 다르면(=csv가 갱신됐으면) 기존 데이터를 지우고 다시 채운다.
 * csv가 안 바뀐 재기동에는 스킵하므로 로컬 테스트 데이터가 매번 날아가지 않는다.
 * Station을 자연키(역명)로, Category/PlaceTag를 자연키(코드/이름)로 참조하므로 StationDataSeeder와 data.sql(Category/PlaceTag 마스터)이 먼저 실행되어 있어야 한다.
 * 파일 IO는 트랜잭션 밖에서 수행하고, DB 저장만 {@link PlaceSeedWriter}의 트랜잭션으로 묶는다.
 */
@Slf4j
@Component
@Profile("!prod")
@Order(3)
@RequiredArgsConstructor
public class PlaceSeeder implements ApplicationRunner {

    private static final String SEED_CSV_PATH = "data/places.csv";
    private static final String PROGRESS_STATUS_DONE = "검수 완료";
    // csv 해시를 기록해두는 마커 파일. build/는 이미 gitignore 대상이라 별도 설정 불필요.
    private static final Path SEED_HASH_MARKER = Path.of("build", "place-seed.sha256");

    private static final String[] CSV_HEADERS = {
            "담당자", "진행 상태", "호선", "역명", "카테고리", "장소명", "해시태그 1", "해시태그 2", "한 줄 설명", "검수메모",
            "주소", "전화번호", "x좌표", "y좌표", "카카오맵 URL"
    };

    private final PlaceRepository placeRepository;
    private final PlaceSeedWriter placeSeedWriter;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        byte[] csvBytes = new ClassPathResource(SEED_CSV_PATH).getInputStream().readAllBytes();
        String currentHash = sha256(csvBytes);
        boolean csvChanged = !currentHash.equals(readStoredHash());

        if (placeRepository.count() > 0 && !csvChanged) {
            log.info("places.csv 변경 없음, place 시딩을 건너뜁니다. count={}", placeRepository.count());
            return;
        }

        log.info("place 시딩 시작(csvChanged={}): source={}", csvChanged, SEED_CSV_PATH);

        List<PlaceSeedRow> rows = readSeedRows(csvBytes);
        placeSeedWriter.write(rows);
        writeStoredHash(currentHash);
    }

    private String readStoredHash() throws IOException {
        if (!Files.exists(SEED_HASH_MARKER)) {
            return "";
        }
        return Files.readString(SEED_HASH_MARKER, StandardCharsets.UTF_8).trim();
    }

    private void writeStoredHash(String hash) throws IOException {
        Files.createDirectories(SEED_HASH_MARKER.getParent());
        Files.writeString(SEED_HASH_MARKER, hash, StandardCharsets.UTF_8);
    }

    private String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }

    private List<PlaceSeedRow> readSeedRows(byte[] csvBytes) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(CSV_HEADERS)
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        List<PlaceSeedRow> rows = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(
                new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8),
                format)) {
            for (CSVRecord record : parser) {
                if (!PROGRESS_STATUS_DONE.equals(record.get("진행 상태").trim())) {
                    continue;
                }
                String placeName = record.get("장소명").trim();
                if (placeName.isBlank()) {
                    continue;
                }

                List<String> hashtagTexts = Stream.of(record.get("해시태그 1"), record.get("해시태그 2"))
                        .map(String::trim)
                        .filter(tag -> !tag.isBlank())
                        .toList();

                rows.add(new PlaceSeedRow(
                        record.get("역명").trim(),
                        record.get("카테고리").trim(),
                        placeName,
                        hashtagTexts,
                        record.get("한 줄 설명").trim(),
                        record.get("주소").trim(),
                        blankToNull(record.get("전화번호")),
                        Double.valueOf(record.get("x좌표").trim()),
                        Double.valueOf(record.get("y좌표").trim()),
                        blankToNull(record.get("카카오맵 URL"))
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
