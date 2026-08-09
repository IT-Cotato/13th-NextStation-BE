package com.cotato.nextstation.domain.place.batch;

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
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 비짓서울(서울관광재단)에서 장소 사진을 수집해 로컬에 내려받는 일회성 배치.
 *
 * <p>이름이 완전히 일치하는 경우만 채택한다. 부분 일치는 오매칭이 섞이므로 자동으로 받지 않고
 * {@code place-photo-output/visitseoul_partial_match.csv}에 기록해 사람이 판단하게 한다.
 * 비짓서울 사진은 출처 표기를 조건으로 이용할 수 있어 출처를 함께 남긴다.
 *
 * <p>수집 결과는 검수를 거친 뒤 {@link PlaceImageUploadBatch}로 업로드한다.
 *
 * <p>실행: {@code ./gradlew placeVisitSeoulFetchBatch}
 */
public final class PlaceVisitSeoulFetchBatch {

    private static final Logger log = LoggerFactory.getLogger(PlaceVisitSeoulFetchBatch.class);

    private static final String BASE_URL = "https://korean.visitseoul.net";
    private static final List<String> SITEMAPS = List.of("restaurants", "attractions", "nature", "shopping");

    private static final Path PHOTO_SOURCE_OUTPUT = Path.of("place-photo-output/place_photo_source.csv");
    private static final Path PARTIAL_MATCH_OUTPUT = Path.of("place-photo-output/visitseoul_partial_match.csv");

    // 업로드 배치가 이 구성을 그대로 읽으므로 컬럼 순서를 변경하지 않는다.
    private static final String[] PHOTO_SOURCE_HEADERS = {
            "카카오 place id", "장소명", "순서", "저장 파일", "저작권 유형", "출처",
            "원본 이미지 URL", "이미지명", "contentid", "관광타입", "관광정보 제목", "거리(m)"
    };
    private static final String[] PARTIAL_HEADERS = {"카카오 place id", "장소명", "역명", "카테고리", "비짓서울 제목", "URL"};

    private static final String SEED_CSV_PATH = "data/places.csv";
    private static final String PROGRESS_STATUS_DONE = "검수 완료";
    private static final String DEFAULT_PHOTO_DIR = "place-photos";
    private static final String SOURCE_LABEL = "비짓서울";

    private static final int MAX_IMAGES_PER_PLACE = 3;
    private static final long REQUEST_INTERVAL_MILLIS = 400; // 공개 웹사이트 대상이므로 요청 간격을 여유 있게 둔다

    private static final Pattern LOC_PATTERN = Pattern.compile("<loc>([^<]+)</loc>");
    private static final Pattern IMAGE_PATTERN = Pattern.compile(
            "/comm/getImage\\?srvcId=MEDIA(?:&amp;|&)parentSn=(\\d+)(?:&amp;|&)fileTy=MEDIA");
    /** URL 끝에 붙는 언어 접미사(-K, -KR 등)를 떼기 위한 패턴. */
    private static final Pattern LANG_SUFFIX = Pattern.compile("-(K|KR|EN|JP|CN)$");
    /** 사진 대신 내려오는 비짓서울 로고 이미지의 MD5. {@link #isPlaceholder} 참고. */
    private static final String PLACEHOLDER_MD5 = "53bac6e13f09c42cbbdd6797c6512ff8";

    private PlaceVisitSeoulFetchBatch() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Path photoDir = Path.of(envOrDefault("PLACE_PHOTO_DIR", DEFAULT_PHOTO_DIR));
        HttpClient httpClient = HttpClient.newHttpClient();

        Map<String, SiteEntry> byName = collectSitemapEntries(httpClient);
        log.info("사이트맵 수집: {}건", byName.size());

        List<SeedRow> rows = readSeedRowsWithoutPhotos(photoDir);
        log.info("사진 없는 장소: {}곳", rows.size());

        List<List<String>> sourceRows = new ArrayList<>();
        List<List<String>> partialRows = new ArrayList<>();
        int downloadedPlaceCount = 0;

        for (SeedRow row : rows) {
            String key = normalize(row.placeName());
            SiteEntry entry = byName.get(key);

            if (entry == null) {
                // 완전일치가 없으면 부분일치 후보만 기록한다. 오매칭 비율이 높아 자동으로 채택하지 않는다.
                findPartial(byName, key).ifPresent(candidate -> partialRows.add(List.of(
                        row.kakaoPlaceId(), row.placeName(), row.stationName(), row.categoryText(),
                        candidate.title(), candidate.url())));
                continue;
            }

            List<String> imageUrls = extractImageUrls(httpClient, entry.url());
            Thread.sleep(REQUEST_INTERVAL_MILLIS);
            if (imageUrls.isEmpty()) {
                log.warn("상세 페이지에 이미지가 없습니다: {} ({})", row.placeName(), entry.url());
                continue;
            }

            Path placeDir = photoDir.resolve(row.kakaoPlaceId());
            int sortOrder = 1;
            for (String imageUrl : imageUrls) {
                if (sortOrder > MAX_IMAGES_PER_PLACE) {
                    break;
                }
                Path target = placeDir.resolve(sortOrder + ".jpg");
                if (!download(httpClient, imageUrl, target)) {
                    continue;
                }
                sourceRows.add(List.of(
                        row.kakaoPlaceId(), row.placeName(), String.valueOf(sortOrder), target.toString(),
                        "", SOURCE_LABEL, imageUrl, entry.title(), "", "", entry.category(), ""));
                sortOrder++;
                Thread.sleep(REQUEST_INTERVAL_MILLIS);
            }

            if (sortOrder > 1) {
                downloadedPlaceCount++;
                log.info("사진 {}장 저장: {} <- {}", sortOrder - 1, row.placeName(), entry.title());
            }
        }

        appendCsv(PHOTO_SOURCE_OUTPUT, PHOTO_SOURCE_HEADERS, sourceRows);
        writeCsv(PARTIAL_MATCH_OUTPUT, PARTIAL_HEADERS, partialRows);
        log.info("비짓서울 수집 완료: 받은 장소={} (사진 {}장), 부분일치 후보={}건",
                downloadedPlaceCount, sourceRows.size(), partialRows.size());
        log.info("부분일치는 오매칭이 섞여 있어 자동 채택하지 않았습니다. {} 확인 후 필요한 것만 수동으로 받으세요.",
                PARTIAL_MATCH_OUTPUT);
    }

    /**
     * 사이트맵에서 정규화한 장소명 -> 항목. URL 경로 마지막 앞 조각이 장소명이라 상세 페이지를 열 필요가 없다.
     * 같은 이름이 여러 개면 먼저 만난 것을 쓴다.
     */
    private static Map<String, SiteEntry> collectSitemapEntries(HttpClient httpClient) throws InterruptedException {
        Map<String, SiteEntry> byName = new LinkedHashMap<>();

        for (String sitemap : SITEMAPS) {
            String xml = getBody(httpClient, BASE_URL + "/sitemap/" + sitemap + "_kr_sitemap.xml");
            if (xml == null) {
                continue;
            }

            int count = 0;
            Matcher matcher = LOC_PATTERN.matcher(xml);
            while (matcher.find()) {
                String url = matcher.group(1).trim();
                String[] parts = url.replaceAll("/+$", "").split("/");
                if (parts.length < 2) {
                    continue;
                }
                String title = LANG_SUFFIX.matcher(
                        URLDecoder.decode(parts[parts.length - 2], StandardCharsets.UTF_8)).replaceAll("");
                byName.putIfAbsent(normalize(title), new SiteEntry(title, url, sitemap));
                count++;
            }
            log.info("  {} 사이트맵: {}건", sitemap, count);
            Thread.sleep(REQUEST_INTERVAL_MILLIS);
        }
        return byName;
    }

    /** 상세 페이지에서 콘텐츠 이미지 URL을 뽑는다. parentSn 기준으로 중복을 없앤다. */
    private static List<String> extractImageUrls(HttpClient httpClient, String pageUrl) {
        String html = getBody(httpClient, pageUrl);
        if (html == null) {
            return List.of();
        }

        Set<String> parentSns = new LinkedHashSet<>();
        Matcher matcher = IMAGE_PATTERN.matcher(html);
        while (matcher.find()) {
            parentSns.add(matcher.group(1));
        }

        List<String> urls = new ArrayList<>();
        for (String parentSn : parentSns) {
            // thumbTy 파라미터를 생략하고 fileNo=1로 요청하면 원본 이미지를 반환한다.
            urls.add(BASE_URL + "/comm/getImage?srvcId=MEDIA&parentSn=" + parentSn + "&fileTy=MEDIA&fileNo=1");
        }
        return urls;
    }

    private static java.util.Optional<SiteEntry> findPartial(Map<String, SiteEntry> byName, String target) {
        if (target.length() < 3) {
            return java.util.Optional.empty();
        }
        return byName.entrySet().stream()
                .filter(e -> e.getKey().length() >= 3 && (e.getKey().contains(target) || target.contains(e.getKey())))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private static String getBody(HttpClient httpClient, String url) {
        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(url))
                            .header("User-Agent", "NextStation/1.0 (cotato 13th project)")
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("응답 실패: status={}, url={}", response.statusCode(), url);
                return null;
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("요청 중 오류: url={}, message={}", url, e.getMessage());
            return null;
        }
    }

    private static boolean download(HttpClient httpClient, String imageUrl, Path target) {
        try {
            HttpResponse<byte[]> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(imageUrl))
                            .header("User-Agent", "NextStation/1.0 (cotato 13th project)")
                            .GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length < 1024) {
                log.warn("이미지 내려받기 실패: status={}, url={}", response.statusCode(), imageUrl);
                return false;
            }
            if (isPlaceholder(response.body())) {
                log.warn("사진이 아닌 사이트 로고라 제외합니다: url={}", imageUrl);
                return false;
            }
            // 폴더는 내려받기에 성공한 뒤에 만든다. 미리 만들면 실패 시 빈 폴더가 남아 이후 실행에서 계속 제외된다.
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

    /**
     * 일부 상세 페이지는 사진 대신 사이트 로고를 media로 노출한다. 로고가 첫 번째 이미지로 저장되면
     * 그대로 썸네일이 되므로 걸러낸다. 로고는 페이지마다 parentSn이 달라 URL로는 구분되지 않아
     * 응답 본문의 해시로 판정한다.
     */
    private static boolean isPlaceholder(byte[] body) {
        try {
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(body));
            return PLACEHOLDER_MD5.equals(digest);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    private record SiteEntry(String title, String url, String category) {
    }

    private record SeedRow(String kakaoPlaceId, String placeName, String stationName, String categoryText) {
    }

    private static List<SeedRow> readSeedRowsWithoutPhotos(Path photoDir) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL)
                .setAllowMissingColumnNames(true)
                .build();

        List<SeedRow> rows = new ArrayList<>();
        try (InputStream stream =
                     PlaceVisitSeoulFetchBatch.class.getClassLoader().getResourceAsStream(SEED_CSV_PATH);
             CSVParser parser = CSVParser.parse(
                     new InputStreamReader(requireStream(stream), StandardCharsets.UTF_8), format)) {
            for (CSVRecord record : parser) {
                if (!PROGRESS_STATUS_DONE.equals(record.get("진행 상태").trim())) {
                    continue;
                }
                String kakaoPlaceUrl = record.get("카카오맵 URL").trim();
                if (kakaoPlaceUrl.isBlank()) {
                    continue;
                }
                String kakaoPlaceId = kakaoPlaceUrl.substring(kakaoPlaceUrl.lastIndexOf('/') + 1);
                if (Files.isDirectory(photoDir.resolve(kakaoPlaceId))) {
                    continue;
                }
                rows.add(new SeedRow(kakaoPlaceId, record.get("장소명").trim(),
                        record.get("역명").trim(), record.get("카테고리").trim()));
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

    private static String normalize(String name) {
        return name.trim().replaceAll("\\s+", "").toLowerCase();
    }

    /** 출처 기록은 다른 배치 결과와 함께 쌓여야 하므로 이어 붙인다. */
    private static void appendCsv(Path path, String[] headers, List<List<String>> rows) throws IOException {
        Files.createDirectories(path.getParent());
        boolean hasContent = Files.exists(path) && Files.size(path) > 0;
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

    private static void writeCsv(Path path, String[] headers, List<List<String>> rows) throws IOException {
        Files.createDirectories(path.getParent());
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(headers).build();
        try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(path, StandardCharsets.UTF_8), format)) {
            for (List<String> row : rows) {
                printer.printRecord(row);
            }
        }
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
