package com.cotato.nextstation.domain.place.batch;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 수집한 장소 사진을 S3에 업로드하고 시딩에 사용할 목록을 생성하는 일회성 배치.
 *
 * <p>{@code place-photos/{카카오 place id}/} 아래의 사진을
 * {@code images/static/places/{카카오 place id}/{순번}.{확장자}} 키로 업로드하고,
 * 업로드 결과를 {@code resources/data/place-images.csv}에 기록한다. 이 파일을 {@code PlaceSeeder}가
 * 읽어 {@code place_image}로 적재하므로, 헤더는 시더와의 계약으로 취급한다.
 *
 * <p>파일명 오름차순이 노출 순서이며 첫 장이 썸네일이 된다. 사진의 출처는 수집 배치가 남긴
 * {@code place_photo_source.csv}에서 가져와 함께 기록한다. 동일한 키에 덮어쓰므로 반복 실행해도
 * 안전하며, 사진을 교체한 뒤 다시 실행하면 그대로 반영된다.
 *
 * <p>실행: {@code ./gradlew placeImageUploadBatch} (환경변수 {@code AWS_S3_BUCKET_NAME} 필요)
 */
public final class PlaceImageUploadBatch {

    private static final Logger log = LoggerFactory.getLogger(PlaceImageUploadBatch.class);

    private static final String DEFAULT_PHOTO_DIR = "place-photos";
    private static final String DEFAULT_REGION = "ap-northeast-2";
    private static final Path IMAGE_LIST_OUTPUT = Path.of("src/main/resources/data/place-images.csv");
    private static final String S3_KEY_PREFIX = "images/static/places";

    // PlaceSeeder가 헤더명으로 조회하므로 문자열을 변경하면 시더도 함께 수정해야 한다.
    private static final String[] IMAGE_LIST_HEADERS = {"카카오 place id", "순서", "이미지 URL", "출처"};

    /** 수집 배치가 남긴 출처 기록. 공공누리 출처표시 의무가 있어 DB까지 이어져야 한다. */
    private static final Path PHOTO_SOURCE_INPUT = Path.of("place-photo-output/place_photo_source.csv");

    private PlaceImageUploadBatch() {
    }

    public static void main(String[] args) throws IOException {
        String bucketName = requireEnv("AWS_S3_BUCKET_NAME");
        String region = envOrDefault("AWS_REGION", DEFAULT_REGION);
        Path photoDir = Path.of(envOrDefault("PLACE_PHOTO_DIR", DEFAULT_PHOTO_DIR));

        if (!Files.isDirectory(photoDir)) {
            throw new IllegalStateException("사진 디렉터리가 없습니다: " + photoDir.toAbsolutePath());
        }

        Map<String, String> sourceByPlaceId = readPhotoSources();
        log.info("출처 기록 로드: {}곳", sourceByPlaceId.size());

        List<List<String>> imageListRows = new ArrayList<>();
        int uploadedCount = 0;
        int skippedCount = 0;
        int missingSourceCount = 0;

        try (S3Client s3Client = S3Client.builder().region(Region.of(region)).build()) {
            for (Path placeDir : listSorted(photoDir)) {
                if (!Files.isDirectory(placeDir)) {
                    continue;
                }

                String kakaoPlaceId = placeDir.getFileName().toString();
                if (!kakaoPlaceId.matches("\\d+")) {
                    log.warn("폴더명이 카카오 place id(숫자)가 아니라 건너뜁니다: {}", placeDir);
                    skippedCount++;
                    continue;
                }

                // 직접 촬영본은 출처표시 의무가 없어 비어 있을 수 있다. API로 수집한 사진이면 출처가 존재해야 한다.
                String source = sourceByPlaceId.getOrDefault(kakaoPlaceId, "");
                if (source.isBlank()) {
                    log.warn("출처 기록이 없습니다(직접 촬영본이 아니라면 확인 필요): {}", kakaoPlaceId);
                    missingSourceCount++;
                }

                int sortOrder = 1;
                for (Path photo : listSorted(placeDir)) {
                    if (!Files.isRegularFile(photo)) {
                        continue;
                    }

                    String extension = extensionOf(photo);
                    String contentType = mapContentType(extension);
                    if (contentType == null) {
                        log.warn("지원하지 않는 확장자라 건너뜁니다: {}", photo);
                        skippedCount++;
                        continue;
                    }

                    String key = "%s/%s/%d.%s".formatted(S3_KEY_PREFIX, kakaoPlaceId, sortOrder, extension);
                    s3Client.putObject(
                            PutObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(key)
                                    .contentType(contentType)
                                    .build(),
                            RequestBody.fromFile(photo)
                    );
                    uploadedCount++;

                    String imageUrl = "https://%s.s3.%s.amazonaws.com/%s".formatted(bucketName, region, key);
                    imageListRows.add(List.of(kakaoPlaceId, String.valueOf(sortOrder), imageUrl, source));
                    sortOrder++;
                }
            }
        }

        writeImageList(imageListRows);
        log.info("장소 사진 업로드 완료: uploadedCount={}, skippedCount={}, 출처없음={}, output={}",
                uploadedCount, skippedCount, missingSourceCount, IMAGE_LIST_OUTPUT);
    }

    /**
     * 수집 배치가 남긴 출처 기록을 카카오 place id별로 읽는다.
     * 한 장소에 여러 장이 있어도 출처는 같으므로 첫 값만 쓴다.
     * 파일이 없으면 빈 맵 — 직접 촬영본만 올리는 경우다.
     */
    private static Map<String, String> readPhotoSources() throws IOException {
        if (!Files.exists(PHOTO_SOURCE_INPUT)) {
            log.info("{} 없음 — 출처 없이 올립니다.", PHOTO_SOURCE_INPUT);
            return Map.of();
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        Map<String, String> sources = new HashMap<>();
        try (CSVParser parser = CSVParser.parse(CsvFiles.bomSafeReader(PHOTO_SOURCE_INPUT), format)) {
            for (CSVRecord record : parser) {
                String id = record.get("카카오 place id").trim();
                String source = record.get("출처").trim();
                if (!id.isBlank() && !source.isBlank()) {
                    sources.putIfAbsent(id, source);
                }
            }
        }
        // 파싱 예외를 처리하지 않고 전파한다. 예외를 무시하면 출처가 비어 있는 목록이 그대로 생성되는데,
        // 공공누리 제1유형은 출처표시가 의무이므로 잘못된 상태로 진행하는 것보다 중단하는 편이 안전하다.
        return sources;
    }

    private static List<Path> listSorted(Path dir) throws IOException {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    private static String extensionOf(Path photo) {
        String fileName = photo.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex < 0 ? "" : fileName.substring(dotIndex + 1).toLowerCase();
    }

    /** 지원하지 않는 확장자는 null. 배치는 한 장 실패로 멈추지 않고 해당 파일만 건너뛴다. */
    private static String mapContentType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> null;
        };
    }

    private static void writeImageList(List<List<String>> rows) throws IOException {
        Files.createDirectories(IMAGE_LIST_OUTPUT.getParent());
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(IMAGE_LIST_HEADERS).build();
        try (CSVPrinter printer = new CSVPrinter(
                Files.newBufferedWriter(IMAGE_LIST_OUTPUT, StandardCharsets.UTF_8), format)) {
            for (List<String> row : rows) {
                printer.printRecord(row);
            }
        }
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("환경변수 " + name + " 가 필요합니다.");
        }
        return value;
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
