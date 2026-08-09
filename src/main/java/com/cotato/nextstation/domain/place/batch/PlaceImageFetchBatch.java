package com.cotato.nextstation.domain.place.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 한국관광공사 TourAPI에서 장소 사진을 수집해 로컬에 내려받는 일회성 배치.
 *
 * <p>사진이 없는 장소를 순회하며 관광정보를 이름으로 매칭하고, 매칭된 콘텐츠의 이미지를
 * {@code place-photos/{카카오 place id}/} 아래에 저장한다. 공공누리 출처표시 의무가 있으므로
 * 사진의 출처를 {@code place-photo-output/place_photo_source.csv}에 함께 기록한다.
 *
 * <p>이름 매칭 특성상 동명의 다른 지점이 섞일 수 있어 수집 결과는 검수를 거친 뒤
 * {@link PlaceImageUploadBatch}로 업로드한다. 이미 사진이 있는 장소는 건너뛰므로
 * 일일 호출 상한에 걸리면 다음 날 이어서 실행하면 된다.
 *
 * <p>실행: {@code ./gradlew placeImageFetchBatch} ({@code TOUR_API_SERVICE_KEY}는 디코딩된 인증키)
 */
public final class PlaceImageFetchBatch {

    private static final Logger log = LoggerFactory.getLogger(PlaceImageFetchBatch.class);

    private static final String DEFAULT_BASE_URL = "https://apis.data.go.kr/B551011/KorService2";
    private static final String LOCATION_BASED_PATH = "/locationBasedList2";
    private static final String DETAIL_IMAGE_PATH = "/detailImage2";
    private static final String AREA_BASED_PATH = "/areaBasedList2";

    // 서울 전역을 타입별로 한 번에 받아 로컬에서 이름을 맞춘다.
    private static final String SEOUL_AREA_CODE = "1";
    private static final String DUMP_CONTENT_TYPE_IDS = "39,12,14,38"; // 음식점, 관광지, 문화시설, 쇼핑
    private static final int AREA_PAGE_SIZE = 100;

    // 관광사진 갤러리는 별도 서비스이고 좌표가 없어 키워드로만 찾는다. 좌표로 못 찾은 장소의 2차 시도
    private static final String DEFAULT_GALLERY_BASE_URL = "https://apis.data.go.kr/B551011/PhotoGalleryService1";
    private static final String GALLERY_SEARCH_PATH = "/gallerySearchList1";

    // Type1: 제1유형(출처표시). Type3: 제1유형 + 변경금지
    private static final String DEFAULT_ALLOWED_COPYRIGHT = "Type1,Type3";

    private static final Path PHOTO_SOURCE_OUTPUT = Path.of("place-photo-output/place_photo_source.csv");
    private static final Path REVIEW_OUTPUT = Path.of("place-photo-output/place_photo_review.csv");

    private static final String[] PHOTO_SOURCE_HEADERS = {
            "카카오 place id", "장소명", "순서", "저장 파일", "저작권 유형", "출처",
            "원본 이미지 URL", "이미지명", "contentid", "관광타입", "관광정보 제목", "거리(m)"
    };
    private static final String[] REVIEW_HEADERS = {"카카오 place id", "장소명", "역명", "사유"};

    private static final String PROGRESS_STATUS_DONE = "검수 완료";
    private static final String SEED_CSV_PATH = "data/places.csv";
    private static final String DEFAULT_PHOTO_DIR = "place-photos";
    private static final String MOBILE_APP = "nextstation";

    // 채택 조건이 이름 일치이므로 반경은 후보를 넓히는 역할만 한다. 반경과 결과 수를 함께 조정한다.
    private static final int SEARCH_RADIUS_METERS = 1000;
    private static final int SEARCH_RESULT_SIZE = 50;

    // 완전 일치는 반경 안이면 채택하지만, 부분 일치는 상위 시설이 걸리기 쉬워 거리와 이름 길이로 함께 제한한다.
    private static final int EXACT_MATCH_MAX_DISTANCE_METERS = 800;
    private static final int PARTIAL_MATCH_MAX_DISTANCE_METERS = 200;
    private static final int PARTIAL_MATCH_MIN_NAME_LENGTH = 3;
    private static final int MAX_IMAGES_PER_PLACE = 3;
    private static final long REQUEST_INTERVAL_MILLIS = 150;
    private static final int DEFAULT_DAILY_CALL_LIMIT = 950; // 개발계정 1,000회에서 여유를 둔다

    private PlaceImageFetchBatch() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        String serviceKey = URLEncoder.encode(requireEnv(), StandardCharsets.UTF_8);
        String baseUrl = envOrDefault("TOUR_API_BASE_URL", DEFAULT_BASE_URL);
        String galleryBaseUrl = envOrDefault("TOUR_API_GALLERY_BASE_URL", DEFAULT_GALLERY_BASE_URL);
        Set<String> allowedCopyright = Set.of(
                envOrDefault("TOUR_API_ALLOWED_COPYRIGHT", DEFAULT_ALLOWED_COPYRIGHT).split(","));
        int callLimit = Integer.parseInt(
                envOrDefault("TOUR_API_DAILY_CALL_LIMIT", String.valueOf(DEFAULT_DAILY_CALL_LIMIT)));
        Path photoDir = Path.of(envOrDefault("PLACE_PHOTO_DIR", DEFAULT_PHOTO_DIR));
        // 두 서비스는 호출 한도가 별도로 집계된다. KorService2 한도를 소진했을 때 관광사진만 이어서 수집한다.
        boolean galleryOnly = Boolean.parseBoolean(envOrDefault("TOUR_API_GALLERY_ONLY", "false"));
        // 서울 전역을 미리 받아 로컬에서 이름을 맞춘다. 좌표 반경 검색으로 찾지 못한 장소를 보완한다.
        boolean areaDump = Boolean.parseBoolean(envOrDefault("TOUR_API_AREA_DUMP", "false"));

        HttpClient httpClient = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();

        List<SeedRow> rows = readSeedRows();
        // 폴더 유무만으로 증분을 판단하면 매 실행이 실패한 장소부터 다시 조회해 뒤쪽 장소까지 한도가 닿지 않는다.
        // 직전 실행의 실패 목록도 함께 제외한다.
        Set<String> alreadyTried = readPreviouslyFailedIds();
        log.info("사진 수집 대상 장소: {}건 (허용 저작권 유형={}, 호출 상한={}, 직전 실패 {}곳 건너뜀)",
                rows.size(), allowedCopyright, callLimit, alreadyTried.size());

        List<JsonNode> dumped = List.of();
        int dumpCallCount = 0;
        if (areaDump) {
            DumpResult dump = dumpSeoulContents(httpClient, objectMapper, baseUrl, serviceKey);
            dumped = dump.items();
            dumpCallCount = dump.callCount();
            log.info("서울 전역 덤프 완료: {}건 (API 호출 {}회)", dumped.size(), dumpCallCount);
        }

        List<List<String>> sourceRows = new ArrayList<>();
        List<List<String>> reviewRows = new ArrayList<>();
        int callCount = dumpCallCount;
        int downloadedPlaceCount = 0;
        int alreadyHaveCount = 0;

        for (SeedRow row : rows) {
            Path placeDir = photoDir.resolve(row.kakaoPlaceId());
            if (Files.isDirectory(placeDir) || alreadyTried.contains(row.kakaoPlaceId())) {
                alreadyHaveCount++;
                continue;
            }

            // 매칭에 성공하면 상세 이미지 조회까지 2회를 사용한다. 조회 도중 한도를 넘지 않도록 미리 중단한다.
            if (callCount + 2 > callLimit) {
                log.warn("일일 호출 상한에 도달해 중단합니다. 내일 다시 실행하면 남은 곳부터 이어집니다. callCount={}", callCount);
                break;
            }

            JsonNode matched = null;
            if (areaDump) {
                // 이미 받아둔 목록에서 조회하므로 API를 호출하지 않는다.
                matched = findInDump(row, dumped);
            } else if (!galleryOnly) {
                matched = findNearbyContent(httpClient, objectMapper, baseUrl, serviceKey, row);
                callCount++;
                Thread.sleep(REQUEST_INTERVAL_MILLIS);
            }

            if (matched == null) {
                if (areaDump) {
                    // 덤프 모드에서는 관광사진 갤러리를 별도 실행으로 이미 처리했으므로 재조회하지 않는다.
                    reviewRows.add(List.of(row.kakaoPlaceId(), row.placeName(), row.stationName(), "덤프에서 매칭 실패"));
                    continue;
                }

                // 좌표 조회로 찾지 못한 장소는 관광사진 갤러리에서 이름으로 재시도한다. 두 서비스의 수록 범위가 다르다.
                int saved = fetchFromGallery(httpClient, objectMapper, galleryBaseUrl, serviceKey, row,
                        placeDir, sourceRows);
                callCount++;
                Thread.sleep(REQUEST_INTERVAL_MILLIS);

                if (saved > 0) {
                    downloadedPlaceCount++;
                    log.info("관광사진 {}장 저장: {}", saved, row.placeName());
                } else {
                    reviewRows.add(List.of(row.kakaoPlaceId(), row.placeName(), row.stationName(),
                            "관광정보·관광사진 모두 매칭 실패"));
                }
                continue;
            }

            String contentId = matched.path("contentid").asText("");
            List<JsonNode> images = fetchImages(httpClient, objectMapper, baseUrl, serviceKey, contentId);
            callCount++;
            Thread.sleep(REQUEST_INTERVAL_MILLIS);

            // 저작권 유형은 콘텐츠 단위가 아니라 이미지 단위로 내려온다. 목록 API 값이 아닌 이 값을 기준으로 거른다.
            List<JsonNode> usable = images.stream()
                    .filter(image -> allowedCopyright.contains(image.path("cpyrhtDivCd").asText("")))
                    .filter(image -> !image.path("originimgurl").asText("").isBlank())
                    .limit(MAX_IMAGES_PER_PLACE)
                    .toList();

            if (usable.isEmpty()) {
                reviewRows.add(List.of(row.kakaoPlaceId(), row.placeName(), row.stationName(),
                        images.isEmpty()
                                ? "상세 이미지 없음 (contentid=" + contentId + ")"
                                : "저작권 유형이 허용 범위 밖 (contentid=" + contentId + ")"));
                continue;
            }

            int sortOrder = 1;
            for (JsonNode image : usable) {
                String imageUrl = image.path("originimgurl").asText("");
                Path target = placeDir.resolve(sortOrder + "." + extensionOf(imageUrl));
                if (!download(httpClient, imageUrl, target)) {
                    continue;
                }

                sourceRows.add(List.of(
                        row.kakaoPlaceId(),
                        row.placeName(),
                        String.valueOf(sortOrder),
                        target.toString(),
                        image.path("cpyrhtDivCd").asText(""),
                        "한국관광공사 TourAPI",
                        imageUrl,
                        image.path("imgname").asText(""),
                        contentId,
                        matched.path("contenttypeid").asText(""),
                        matched.path("title").asText(""),
                        matched.path("dist").asText("")
                ));
                sortOrder++;
            }

            if (sortOrder > 1) {
                downloadedPlaceCount++;
                log.info("사진 {}장 저장: {} <- {} ({}m)", sortOrder - 1, row.placeName(),
                        matched.path("title").asText(""), matched.path("dist").asText(""));
            } else {
                reviewRows.add(List.of(row.kakaoPlaceId(), row.placeName(), row.stationName(), "이미지 내려받기 실패"));
            }
        }

        writeCsv(PHOTO_SOURCE_OUTPUT, PHOTO_SOURCE_HEADERS, sourceRows, true);
        // 실패 목록은 이어 붙인다. 덮어쓰면 다음 실행이 이전 실패 장소를 다시 조회해 호출 한도를 소모한다.
        writeCsv(REVIEW_OUTPUT, REVIEW_HEADERS, reviewRows, true);
        log.info("사진 수집 완료: 받은 장소={} (사진 {}장), 이미 있어 건너뜀={}, 못 받음={}, API 호출={}",
                downloadedPlaceCount, sourceRows.size(), alreadyHaveCount, reviewRows.size(), callCount);
        log.info("사람 검수 후 placeImageUploadBatch로 올리세요. 출처 기록={}, 실패 사유={}",
                PHOTO_SOURCE_OUTPUT, REVIEW_OUTPUT);
    }

    /**
     * 좌표 반경 안에서 장소명이 일치하는 관광정보를 찾는다.
     * 거리만으로 고르면 옆 건물 사진이 붙으므로, 이름이 맞지 않으면 채택하지 않는다.
     * arrange=S는 대표 이미지가 있는 항목만 거리순으로 반환한다.
     *
     * <p>부분 일치는 상위 시설이 걸리기 쉬우므로({@code 북카페 … 암사종합시장점} → {@code 암사종합시장})
     * {@link #PARTIAL_MATCH_MAX_DISTANCE_METERS} 안에 있을 때만 채택하고, 완전 일치를 우선한다.
     * 판정 기준은 {@link #findInDump}와 동일하게 유지한다.
     */
    private static JsonNode findNearbyContent(HttpClient httpClient, ObjectMapper objectMapper,
                                              String baseUrl, String serviceKey, SeedRow row) {
        String url = baseUrl + LOCATION_BASED_PATH
                + "?serviceKey=" + serviceKey
                + "&MobileOS=ETC&MobileApp=" + MOBILE_APP + "&_type=json"
                + "&mapX=" + row.xCoordinate()
                + "&mapY=" + row.yCoordinate()
                + "&radius=" + SEARCH_RADIUS_METERS
                + "&numOfRows=" + SEARCH_RESULT_SIZE
                + "&pageNo=1"
                + "&arrange=S";

        String target = normalizePlaceName(row.placeName());
        JsonNode partialCandidate = null;

        // 거리순으로 내려오므로 먼저 만난 항목이 가장 가깝다. 완전 일치가 나오면 그 자리에서 채택한다.
        for (JsonNode item : requestItems(httpClient, objectMapper, url)) {
            String title = normalizePlaceName(item.path("title").asText(""));
            if (title.isBlank()) {
                continue;
            }
            if (title.equals(target)) {
                return item;
            }
            if (partialCandidate == null && isPartialMatch(title, target)
                    && item.path("dist").asDouble(Double.MAX_VALUE) <= PARTIAL_MATCH_MAX_DISTANCE_METERS) {
                partialCandidate = item;
            }
        }
        return partialCandidate;
    }

    /** 한두 글자가 우연히 겹치는 것을 배제하기 위해 최소 길이를 둔다. */
    private static boolean isPartialMatch(String title, String target) {
        return target.length() >= PARTIAL_MATCH_MIN_NAME_LENGTH
                && (title.contains(target) || target.contains(title));
    }

    /**
     * 직전 실행에서 사진을 못 받은 장소의 카카오 place id.
     * 다시 시도하고 싶으면 {@link #REVIEW_OUTPUT} 파일을 지우면 된다.
     */
    private static Set<String> readPreviouslyFailedIds() throws IOException {
        if (!Files.exists(REVIEW_OUTPUT)) {
            return Set.of();
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        Set<String> ids = new HashSet<>();
        try (CSVParser parser = CSVParser.parse(CsvFiles.bomSafeReader(REVIEW_OUTPUT), format)) {
            for (CSVRecord record : parser) {
                String id = record.get("카카오 place id").trim();
                if (!id.isBlank()) {
                    ids.add(id);
                }
            }
        }
        // 파싱 예외를 무시하면 실패 목록이 빈 집합이 되어 모든 장소를 다시 조회하고 호출 한도를 소진한다.
        return ids;
    }

    private record DumpResult(List<JsonNode> items, int callCount) {
    }

    /**
     * 서울 전역의 관광정보를 타입별로 전량 받아둔다.
     *
     * <p>{@code arrange=O}는 대표 이미지가 반드시 있는 항목만 제목순으로 주므로, 사진 없는 콘텐츠를 애초에 안 받는다.
     * 한 번 받아두면 그 뒤로는 API 없이 매칭 조건만 바꿔가며 실험할 수 있다.
     */
    private static DumpResult dumpSeoulContents(HttpClient httpClient, ObjectMapper objectMapper,
                                                String baseUrl, String serviceKey) throws InterruptedException {
        List<JsonNode> all = new ArrayList<>();
        int callCount = 0;

        for (String contentTypeId : DUMP_CONTENT_TYPE_IDS.split(",")) {
            int received = 0;
            for (int pageNo = 1; pageNo <= 200; pageNo++) {
                String url = baseUrl + AREA_BASED_PATH
                        + "?serviceKey=" + serviceKey
                        + "&MobileOS=ETC&MobileApp=" + MOBILE_APP + "&_type=json"
                        + "&areaCode=" + SEOUL_AREA_CODE
                        + "&contentTypeId=" + contentTypeId
                        + "&numOfRows=" + AREA_PAGE_SIZE
                        + "&pageNo=" + pageNo
                        + "&arrange=O";

                List<JsonNode> page = requestItems(httpClient, objectMapper, url);
                callCount++;
                Thread.sleep(REQUEST_INTERVAL_MILLIS);

                if (page.isEmpty()) {
                    break;
                }
                all.addAll(page);
                received += page.size();
                if (page.size() < AREA_PAGE_SIZE) {
                    break;
                }
            }
            log.info("contentTypeId={} 수신 {}건", contentTypeId, received);
        }
        return new DumpResult(all, callCount);
    }

    /**
     * 미리 받아둔 목록에서 이름이 맞고 좌표도 가까운 항목을 찾는다.
     * 서울에 같은 상호가 여럿이라 이름만으로는 위험해 좌표로 한 번 더 거른다.
     */
    private static JsonNode findInDump(SeedRow row, List<JsonNode> dumped) {
        String target = normalizePlaceName(row.placeName());
        double placeLat = Double.parseDouble(row.yCoordinate());
        double placeLon = Double.parseDouble(row.xCoordinate());

        JsonNode best = null;
        double bestDistance = Double.MAX_VALUE;
        boolean bestExact = false;

        for (JsonNode item : dumped) {
            String title = normalizePlaceName(item.path("title").asText(""));
            if (title.isBlank()) {
                continue;
            }
            boolean exact = title.equals(target);
            if (!exact && !isPartialMatch(title, target)) {
                continue;
            }

            double lat = item.path("mapy").asDouble(0);
            double lon = item.path("mapx").asDouble(0);
            if (lat == 0 || lon == 0) {
                continue;
            }
            // 서울 위도(약 37.5) 기준 평면 근사. 수백 m 단위 판정에는 충분한 정밀도다.
            double dy = (placeLat - lat) * 111_000;
            double dx = (placeLon - lon) * 88_300;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance > (exact ? EXACT_MATCH_MAX_DISTANCE_METERS : PARTIAL_MATCH_MAX_DISTANCE_METERS)) {
                continue;
            }

            if (best == null || (exact && !bestExact) || (exact == bestExact && distance < bestDistance)) {
                best = item;
                bestDistance = distance;
                bestExact = exact;
            }
        }
        return best;
    }

    /**
     * 관광사진 갤러리에서 장소명으로 사진을 찾아 내려받는다. 저장한 장수를 준다.
     *
     * <p>이 서비스는 좌표가 없어 키워드로만 찾을 수 있다. 그래서 제목·촬영지·검색키워드 중 하나에
     * 장소명이 들어 있는 항목만 채택한다 — 검색어가 느슨하게 걸려 엉뚱한 사진이 오는 걸 막는다.
     * 전체가 공공누리 1유형이라 저작권 유형을 따로 거르지 않는다.
     */
    private static int fetchFromGallery(HttpClient httpClient, ObjectMapper objectMapper,
                                        String galleryBaseUrl, String serviceKey, SeedRow row,
                                        Path placeDir, List<List<String>> sourceRows) {
        String url = galleryBaseUrl + GALLERY_SEARCH_PATH
                + "?serviceKey=" + serviceKey
                + "&MobileOS=ETC&MobileApp=" + MOBILE_APP + "&_type=json"
                + "&keyword=" + URLEncoder.encode(row.placeName(), StandardCharsets.UTF_8)
                + "&numOfRows=" + MAX_IMAGES_PER_PLACE * 3
                + "&pageNo=1";

        String target = normalizePlaceName(row.placeName());
        int sortOrder = 1;

        for (JsonNode item : requestItems(httpClient, objectMapper, url)) {
            if (sortOrder > MAX_IMAGES_PER_PLACE) {
                break;
            }

            // 이 API는 좌표를 제공하지 않아 촬영지 텍스트로 서울 여부를 판정한다.
            // 지역을 확인하지 않으면 동명의 다른 지역 사진이 매칭된다(예: 가게 "묵호" → 강원도 묵호항).
            String location = item.path("galPhotographyLocation").asText("");
            if (!location.contains("서울")) {
                continue;
            }

            String haystack = normalizePlaceName(item.path("galTitle").asText("")
                    + location + item.path("galSearchKeyword").asText(""));
            if (!haystack.contains(target)) {
                continue;
            }

            String imageUrl = item.path("galWebImageUrl").asText("");
            if (imageUrl.isBlank()) {
                continue;
            }

            Path targetPath = placeDir.resolve(sortOrder + "." + extensionOf(imageUrl));
            if (!download(httpClient, imageUrl, targetPath)) {
                continue;
            }

            sourceRows.add(List.of(
                    row.kakaoPlaceId(),
                    row.placeName(),
                    String.valueOf(sortOrder),
                    targetPath.toString(),
                    "공공누리 1유형",
                    "한국관광공사 관광사진갤러리 / " + item.path("galPhotographer").asText(""),
                    imageUrl,
                    item.path("galTitle").asText(""),
                    item.path("galContentId").asText(""),
                    "",
                    item.path("galPhotographyLocation").asText(""),
                    ""
            ));
            sortOrder++;
        }
        return sortOrder - 1;
    }

    /** 콘텐츠의 이미지 목록. 응답 항목마다 cpyrhtDivCd(저작권 유형)가 따로 붙는다. */
    private static List<JsonNode> fetchImages(HttpClient httpClient, ObjectMapper objectMapper,
                                              String baseUrl, String serviceKey, String contentId) {
        String url = baseUrl + DETAIL_IMAGE_PATH
                + "?serviceKey=" + serviceKey
                + "&MobileOS=ETC&MobileApp=" + MOBILE_APP + "&_type=json"
                + "&contentId=" + URLEncoder.encode(contentId, StandardCharsets.UTF_8)
                + "&imageYN=Y"
                + "&numOfRows=" + MAX_IMAGES_PER_PLACE * 3
                + "&pageNo=1";

        return requestItems(httpClient, objectMapper, url);
    }

    /**
     * 응답 껍데기(response.body.items.item)를 벗겨 항목 목록을 준다.
     * TourAPI는 결과가 1건이면 배열이 아니라 객체로 주고, 0건이면 items가 빈 문자열로 오기도 한다.
     */
    private static List<JsonNode> requestItems(HttpClient httpClient, ObjectMapper objectMapper, String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("TourAPI 응답 실패: status={}", response.statusCode());
                return List.of();
            }

            JsonNode body = objectMapper.readTree(response.body()).path("response");
            String resultCode = body.path("header").path("resultCode").asText("");
            if (!resultCode.isBlank() && !"0000".equals(resultCode)) {
                log.warn("TourAPI 오류 응답: resultCode={}, resultMsg={}",
                        resultCode, body.path("header").path("resultMsg").asText(""));
                return List.of();
            }

            JsonNode item = body.path("body").path("items").path("item");
            if (item.isArray()) {
                List<JsonNode> items = new ArrayList<>();
                item.forEach(items::add);
                return items;
            }
            return item.isObject() ? List.of(item) : List.of();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("TourAPI 호출 중 오류: {}", e.getMessage());
            return List.of();
        }
    }

    private static boolean download(HttpClient httpClient, String imageUrl, Path target) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl)).GET().build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.warn("이미지 내려받기 실패: status={}, url={}", response.statusCode(), imageUrl);
                return false;
            }
            // 폴더는 내려받기에 성공한 뒤에 만든다. 미리 만들면 실패 시 빈 폴더가 남고,
            // 증분 판정이 폴더 유무 기준이므로 해당 장소가 이후 실행에서 계속 제외된다.
            Files.createDirectories(target.getParent());
            Files.write(target, response.body());
            return true;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("이미지 내려받기 중 오류: url={}, message={}", imageUrl, e.getMessage());
            return false;
        }
    }

    private record SeedRow(String kakaoPlaceId, String placeName, String stationName,
                           String xCoordinate, String yCoordinate) {
    }

    /**
     * 시트 컬럼이 앞에 추가돼도 안 깨지도록 첫 줄을 헤더로 읽는다.
     * places.csv 끝에 이름 없는 빈 컬럼이 있어 중복·누락 헤더를 허용해야 한다.
     */
    private static List<SeedRow> readSeedRows() throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL)
                .setAllowMissingColumnNames(true)
                .build();

        List<SeedRow> rows = new ArrayList<>();
        try (InputStream stream = PlaceImageFetchBatch.class.getClassLoader().getResourceAsStream(SEED_CSV_PATH);
             CSVParser parser = CSVParser.parse(
                     new InputStreamReader(requireStream(stream), StandardCharsets.UTF_8), format)) {
            for (CSVRecord record : parser) {
                if (!PROGRESS_STATUS_DONE.equals(record.get("진행 상태").trim())) {
                    continue;
                }
                String kakaoPlaceUrl = record.get("카카오맵 URL").trim();
                String xCoordinate = record.get("x좌표").trim();
                String yCoordinate = record.get("y좌표").trim();
                if (kakaoPlaceUrl.isBlank() || xCoordinate.isBlank() || yCoordinate.isBlank()) {
                    continue;
                }

                rows.add(new SeedRow(
                        kakaoPlaceUrl.substring(kakaoPlaceUrl.lastIndexOf('/') + 1),
                        record.get("장소명").trim(),
                        record.get("역명").trim(),
                        xCoordinate,
                        yCoordinate
                ));
            }
        }
        return rows;
    }

    private static InputStream requireStream(InputStream stream) {
        if (stream == null) {
            throw new IllegalStateException("클래스패스에서 " + SEED_CSV_PATH + " 를 찾을 수 없습니다.");
        }
        return stream;
    }

    private static String normalizePlaceName(String name) {
        return name.trim().replaceAll("\\s+", "");
    }

    private static String extensionOf(String imageUrl) {
        String path = imageUrl.split("\\?")[0];
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex < path.lastIndexOf('/')) {
            return "jpg";
        }
        return switch (path.substring(dotIndex + 1).toLowerCase()) {
            case "jpeg" -> "jpeg";
            case "png" -> "png";
            case "webp" -> "webp";
            default -> "jpg";
        };
    }

    private static void writeCsv(Path path, String[] headers, List<List<String>> rows, boolean append)
            throws IOException {
        Files.createDirectories(path.getParent());
        try {
            printCsv(path, headers, rows, append);
        } catch (IOException e) {
            Path fallback = path.resolveSibling(path.getFileName() + "." + System.currentTimeMillis() + ".csv");
            log.warn("{} 쓰기 실패({}) — {}에 대신 기록합니다. 원래 파일을 닫고 내용을 옮기세요.",
                    path, e.getMessage(), fallback);
            printCsv(fallback, headers, rows, false);
        }
    }

    /**
     * 출처 기록은 실행할 때마다 이어 붙인다. 덮어쓰면 이전 실행에서 받은 사진의 저작권 근거가 사라진다.
     * 헤더는 파일을 새로 만들 때만 쓴다.
     */
    private static void printCsv(Path path, String[] headers, List<List<String>> rows, boolean append)
            throws IOException {
        boolean hasContent = append && Files.exists(path) && Files.size(path) > 0;
        CSVFormat.Builder builder = CSVFormat.DEFAULT.builder();
        if (!hasContent) {
            builder.setHeader(headers);
        }

        try (CSVPrinter printer = new CSVPrinter(
                hasContent
                        ? Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND)
                        : Files.newBufferedWriter(path, StandardCharsets.UTF_8),
                builder.build())) {
            for (List<String> row : rows) {
                printer.printRecord(row);
            }
        }
    }

    private static String requireEnv() {
        String value = System.getenv("TOUR_API_SERVICE_KEY");
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("환경변수 TOUR_API_SERVICE_KEY 가 필요합니다.");
        }
        return value;
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
