package com.cotato.nextstation.domain.place.init;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * place-images.csv를 읽어 장소에 붙이는 경로만 검증한다.
 * DB 저장은 PlaceSeedWriter 책임이라 여기서는 리포지토리를 쓰지 않는다.
 */
class PlaceSeederTest {

    private static final String BASE_URL = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/static/places";

    private final PlaceSeeder placeSeeder = new PlaceSeeder(null, null);

    @Test
    @DisplayName("이미지 목록 csv를 카카오 place id별로 순서 컬럼 기준 정렬해 묶는다")
    void readImagesGroupsByKakaoPlaceIdInSortOrder() throws IOException {
        // 2번 사진이 먼저 오도록 일부러 섞어둔다. 행 순서가 아니라 "순서" 컬럼을 따라야 한다.
        String csv = """
                카카오 place id,순서,이미지 URL,출처
                1584284345,2,%s/1584284345/2.jpg,한국관광공사
                1584284345,1,%s/1584284345/1.jpg,한국관광공사
                27334082,1,%s/27334082/1.jpg,
                """.formatted(BASE_URL, BASE_URL, BASE_URL);

        Map<String, List<PlaceSeedImage>> result = placeSeeder.readImages(csv.getBytes(StandardCharsets.UTF_8));

        assertThat(result).hasSize(2);
        // 첫 장이 썸네일(sortOrder 0)이 되므로 순서가 뒤집히면 대표 이미지가 바뀐다.
        assertThat(result.get("1584284345")).containsExactly(
                new PlaceSeedImage(BASE_URL + "/1584284345/1.jpg", "한국관광공사"),
                new PlaceSeedImage(BASE_URL + "/1584284345/2.jpg", "한국관광공사"));
        // 출처가 비어 있으면 null — 직접 촬영본은 출처표시 의무가 없다.
        assertThat(result.get("27334082")).containsExactly(
                new PlaceSeedImage(BASE_URL + "/27334082/1.jpg", null));
    }

    @Test
    @DisplayName("이미지 목록 csv가 없으면 빈 맵을 준다")
    void readImagesReturnsEmptyWhenNoFile() throws IOException {
        assertThat(placeSeeder.readImages(new byte[0])).isEmpty();
    }

    @Test
    @DisplayName("카카오맵 URL의 마지막 세그먼트를 place id로 쓴다")
    void extractKakaoPlaceIdUsesLastSegment() {
        assertThat(placeSeeder.extractKakaoPlaceId("https://place.map.kakao.com/1584284345"))
                .isEqualTo("1584284345");
    }

    @Test
    @DisplayName("카카오맵 URL이 없는 장소는 사진을 붙일 키가 없다")
    void extractKakaoPlaceIdReturnsNullWhenUrlMissing() {
        assertThat(placeSeeder.extractKakaoPlaceId(null)).isNull();
        assertThat(placeSeeder.extractKakaoPlaceId("   ")).isNull();
    }
}
