package com.cotato.nextstation.domain.place.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * "장소 데이터" 구글 시트를 읽어 카카오 로컬 "키워드로 장소 검색" API로
 * 주소/좌표/카카오맵 URL을 보강하는 1회성 배치 스크립트
 */
public final class PlaceGeocodingBatch {

    private static final Logger log = LoggerFactory.getLogger(PlaceGeocodingBatch.class);

    private static final String SHEET_CSV_URL =
            "https://docs.google.com/spreadsheets/d/e/2PACX-1vQqA_XMVkMvrZuryHHE-yccZ3hThynyL3kk6qUoeXDMOLakTxY1N_DrPJhSp6WLemdo2zPzLHJpWA95/pub?gid=0&single=true&output=csv";
    private static final String KAKAO_KEYWORD_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final Path ENRICHED_OUTPUT = Path.of("place-geocoding-output/place_geocoded.csv");
    private static final Path MANUAL_REVIEW_OUTPUT = Path.of("place-geocoding-output/place_manual_review.csv");
    private static final long REQUEST_INTERVAL_MILLIS = 150;
    private static final int MAX_RETRY = 3;

    private static final String[] OUTPUT_HEADERS = {
            "포함 여부", "노선", "역명", "카테고리", "장소명", "해시태그", "설명",
            "주소", "전화번호", "x좌표", "y좌표", "kakao_place_url"
    };

    private PlaceGeocodingBatch() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String kakaoApiKey = System.getenv("KAKAO_REST_API_KEY");
        if (kakaoApiKey == null || kakaoApiKey.isBlank()) {
            throw new IllegalStateException("환경변수 KAKAO_REST_API_KEY가 설정되어 있지 않습니다. .env.local 값을 실행 환경에 로드했는지 확인하세요.");
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        ObjectMapper objectMapper = new ObjectMapper();

        verifyKakaoApiKey(httpClient, kakaoApiKey);

        List<CSVRecord> targetRows = downloadIncludedRows(httpClient);
        log.info("포함 여부 체크된 대상 행 {}건 로드", targetRows.size());

        List<Map<String, String>> enrichedRows = new ArrayList<>();
        List<Map<String, String>> manualReviewRows = new ArrayList<>();

        int processed = 0;
        int fallbackResolved = 0;
        for (CSVRecord row : targetRows) {
            String stationName = row.get("역명").trim();
            String placeName = row.get("장소명").trim();

            Resolution resolution = resolveWithQuery(httpClient, objectMapper, kakaoApiKey,
                    stationName + " " + placeName, placeName);

            if (!resolution.confirmed() && !"AMBIGUOUS".equals(resolution.reasonPrefix())) {
                Thread.sleep(REQUEST_INTERVAL_MILLIS);
                Resolution fallback = resolveWithQuery(httpClient, objectMapper, kakaoApiKey, placeName, placeName);
                if (fallback.confirmed() || fallback.candidates().size() > resolution.candidates().size()) {
                    if (fallback.confirmed()) {
                        fallbackResolved++;
                        log.info("[재검증 필요] 역명 없이 확정됨: {} {} -> {}({})",
                                stationName, placeName,
                                fallback.match().path("place_name").asText(""),
                                fallback.match().path("address_name").asText(""));
                    }
                    resolution = fallback;
                }
            }

            if (resolution.confirmed()) {
                enrichedRows.add(toEnrichedRow(row, resolution.match()));
            } else {
                manualReviewRows.add(toManualReviewRow(row, resolution.reason(), resolution.candidates()));
            }

            processed++;
            if (processed % 50 == 0) {
                log.info("진행 {}/{}", processed, targetRows.size());
            }

            Thread.sleep(REQUEST_INTERVAL_MILLIS);
        }
        log.info("역명 없이 재검색해서 추가로 확정된 건수: {}", fallbackResolved);

        writeCsv(ENRICHED_OUTPUT, OUTPUT_HEADERS, enrichedRows);
        writeCsv(MANUAL_REVIEW_OUTPUT,
                new String[]{"역명", "장소명", "카테고리", "사유", "후보"},
                manualReviewRows);

        log.info("완료: 확정 {}건 -> {}, 수동 확인 {}건 -> {}",
                enrichedRows.size(), ENRICHED_OUTPUT, manualReviewRows.size(), MANUAL_REVIEW_OUTPUT);
    }

    private static void verifyKakaoApiKey(HttpClient httpClient, String kakaoApiKey)
            throws IOException, InterruptedException {
        URI uri = URI.create(KAKAO_KEYWORD_SEARCH_URL + "?query=" + URLEncoder.encode("서울역", StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", "KakaoAK " + kakaoApiKey)
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IOException("카카오 API 연결 확인 실패", e);
        }

        if (response.statusCode() == 401) {
            throw new IllegalStateException(
                    "카카오 API 키가 유효하지 않습니다 (401 Unauthorized). KAKAO_REST_API_KEY 값을 확인하세요.");
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("카카오 API 사전 점검 실패: HTTP " + response.statusCode() + " - " + response.body());
        }

        log.info("카카오 API 키 확인 완료");
    }

    private static List<CSVRecord> downloadIncludedRows(HttpClient httpClient) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(SHEET_CSV_URL)).GET().build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IOException("구글 시트 CSV 다운로드 실패: " + SHEET_CSV_URL, e);
        }
        if (response.statusCode() != 200) {
            throw new IOException("구글 시트 CSV 다운로드 실패: HTTP " + response.statusCode());
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(OUTPUT_HEADERS)
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        try (CSVParser parser = CSVParser.parse(new StringReader(response.body()), format)) {
            List<CSVRecord> result = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (!"TRUE".equalsIgnoreCase(record.get("포함 여부").trim())) {
                    continue;
                }
                if (record.get("장소명").isBlank()) {
                    continue;
                }
                result.add(record);
            }
            return result;
        }
    }

    private static JsonNode searchKakaoKeyword(HttpClient httpClient, ObjectMapper objectMapper,
                                                String kakaoApiKey, String query) throws InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        URI uri = URI.create(KAKAO_KEYWORD_SEARCH_URL + "?query=" + encodedQuery + "&size=15&page=1");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", "KakaoAK " + kakaoApiKey)
                .GET()
                .build();

        long backoffMillis = 300;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200) {
                    return objectMapper.readTree(response.body()).path("documents");
                }
                if (response.statusCode() == 429) {
                    log.warn("카카오 API 요청 제한(429), {}ms 후 재시도 (query={})", backoffMillis, query);
                    Thread.sleep(backoffMillis);
                    backoffMillis *= 2;
                    continue;
                }
                if (response.statusCode() == 401) {
                    throw new IllegalStateException("카카오 API 키가 처리 도중 무효화되었습니다 (401). 중단합니다.");
                }
                log.warn("카카오 API 오류 응답: status={}, query={}", response.statusCode(), query);
                return null;
            } catch (IOException e) {
                log.warn("카카오 API 호출 실패(query={}, attempt={}): {}", query, attempt, e.getMessage());
                Thread.sleep(backoffMillis);
                backoffMillis *= 2;
            }
        }
        return null;
    }

    private record Resolution(boolean confirmed, JsonNode match, String reason, List<String> candidates) {
        String reasonPrefix() {
            int idx = reason.indexOf('(');
            return idx >= 0 ? reason.substring(0, idx) : reason;
        }
    }

    private static Resolution resolveWithQuery(HttpClient httpClient, ObjectMapper objectMapper,
                                                String kakaoApiKey, String query, String placeName) throws InterruptedException {
        JsonNode documents = searchKakaoKeyword(httpClient, objectMapper, kakaoApiKey, query);
        if (documents == null) {
            return new Resolution(false, null, "API_ERROR", List.of());
        }
        if (documents.isEmpty()) {
            return new Resolution(false, null, "NO_RESULT", List.of());
        }
        List<JsonNode> exactMatches = findExactNameMatches(documents, placeName);
        if (exactMatches.size() == 1) {
            return new Resolution(true, exactMatches.get(0), null, List.of());
        }
        if (exactMatches.isEmpty()) {
            return new Resolution(false, null, "NO_EXACT_MATCH", toCandidateList(documents));
        }
        return new Resolution(false, null, "AMBIGUOUS(" + exactMatches.size() + ")", toCandidateList(exactMatches));
    }

    private static List<JsonNode> findExactNameMatches(JsonNode documents, String placeName) {
        String normalizedTarget = normalizePlaceName(placeName);
        List<JsonNode> matches = new ArrayList<>();
        for (JsonNode document : documents) {
            if (normalizePlaceName(document.path("place_name").asText("")).equals(normalizedTarget)) {
                matches.add(document);
            }
        }
        return matches;
    }

    private static String normalizePlaceName(String name) {
        return name.trim().replaceAll("\\s+", "");
    }

    private static Map<String, String> toEnrichedRow(CSVRecord row, JsonNode document) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("포함 여부", row.get("포함 여부"));
        result.put("노선", row.get("노선"));
        result.put("역명", row.get("역명"));
        result.put("카테고리", row.get("카테고리"));
        result.put("장소명", row.get("장소명"));
        result.put("해시태그", row.get("해시태그"));
        result.put("설명", row.get("설명"));
        result.put("주소", document.path("address_name").asText(""));
        result.put("전화번호", row.get("전화번호").isBlank() ? document.path("phone").asText("") : row.get("전화번호"));
        result.put("x좌표", document.path("x").asText(""));
        result.put("y좌표", document.path("y").asText(""));
        result.put("kakao_place_url", document.path("place_url").asText(""));
        return result;
    }

    private static Map<String, String> toManualReviewRow(CSVRecord row, String reason, List<String> candidates) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("역명", row.get("역명"));
        result.put("장소명", row.get("장소명"));
        result.put("카테고리", row.get("카테고리"));
        result.put("사유", reason);
        result.put("후보", String.join(" | ", candidates));
        return result;
    }

    private static List<String> toCandidateList(Iterable<JsonNode> documents) {
        return StreamSupport.stream(documents.spliterator(), false)
                .limit(5)
                .map(doc -> doc.path("place_name").asText("") + "(" + doc.path("address_name").asText("") + ")")
                .collect(Collectors.toList());
    }

    private static void writeCsv(Path path, String[] headers, List<Map<String, String>> rows) throws IOException {
        Files.createDirectories(path.getParent());
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(headers).build();
        try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(path, StandardCharsets.UTF_8), format)) {
            for (Map<String, String> row : rows) {
                List<String> values = new ArrayList<>();
                for (String header : headers) {
                    values.add(row.getOrDefault(header, ""));
                }
                printer.printRecord(values);
            }
        }
    }
}