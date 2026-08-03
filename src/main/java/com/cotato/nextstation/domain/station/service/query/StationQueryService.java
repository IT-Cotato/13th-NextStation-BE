package com.cotato.nextstation.domain.station.service.query;

import com.cotato.nextstation.domain.place.dto.response.PlaceInfoResponse;
import com.cotato.nextstation.domain.place.service.query.PlaceInfoQueryService;
import com.cotato.nextstation.domain.place.service.query.PlaceQueryService;
import com.cotato.nextstation.domain.station.converter.LineConverter;
import com.cotato.nextstation.domain.station.converter.StationConverter;
import com.cotato.nextstation.domain.station.dto.response.StationPlaceCategoryResponse;
import com.cotato.nextstation.domain.station.dto.response.StationPlacesResponse;
import com.cotato.nextstation.domain.station.dto.response.LineSummaryResponse;
import com.cotato.nextstation.domain.station.dto.response.StationSummaryResponse;
import com.cotato.nextstation.domain.station.entity.Station;
import com.cotato.nextstation.domain.station.exception.StationErrorCode;
import com.cotato.nextstation.domain.station.repository.StationLineRepository;
import com.cotato.nextstation.domain.station.repository.StationLineRepository.StationLineView;
import com.cotato.nextstation.domain.station.repository.StationRepository;
import com.cotato.nextstation.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

// 역 조회 전용 서비스. 다른 도메인이 역 정보가 필요할 때 이 서비스를 호출한다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StationQueryService {

    // 코스 만들기 화면의 카테고리 노출 순서(문화공간 → 식당 → 카페 → 산책포인트).
    // 랜덤뽑기 코스 미리보기와 같은 순서를 쓴다.
    private static final List<String> CATEGORY_DISPLAY_ORDER = List.of("CULTURE", "FOOD", "CAFE", "WALK");
    private static final int PLACES_PER_CATEGORY = 3;
    private static final String COURSE_NAME_SUFFIX = " 환승여행 코스";

    // 역 검색 결과 상한. 짧은 검색어에 수백 건이 쏟아지는 것을 막는다.
    private static final int STATION_SEARCH_LIMIT = 20;

    // 모든 역명이 이 접미사로 끝나 검색어로서 변별력이 없다. 검색 시 양쪽에서 뗀다.
    private static final String STATION_NAME_SUFFIX = "역";

    // LIKE 이스케이프 문자. 역명에 쓰이지 않고 Java·JPQL·MySQL에서 중복 이스케이프될 일도 없다.
    private static final String LIKE_ESCAPE = "!";

    private final StationRepository stationRepository;
    private final StationLineRepository stationLineRepository;
    private final PlaceQueryService placeQueryService;
    private final PlaceInfoQueryService placeInfoQueryService;
    private final StationConverter stationConverter;
    private final LineConverter lineConverter;

    /**
     * 코스 만들기 후보 장소를 카테고리별로 묶어 반환한다.
     * 카테고리당 최대 {@value #PLACES_PER_CATEGORY}개이며, 후보가 더 많으면 잘라내고 적으면 있는 만큼 내린다.
     * 뽑기 대상이 아닌 역은 빈 목록이 나가며, 역 자체가 없으면 404다.
     *
     * @param travelStyles 비어 있으면(랜덤뽑기 등 일반 진입) 호출할 때마다 같은 결과가 나오도록 id 순으로 고정한다.
     *                     값이 있으면(맞춤추천 결과 화면 진입) 카테고리 안에서 선택한 태그와 매칭되는 개수가
     *                     많은 장소부터 우선 노출하고, 매칭 개수가 같으면 무작위로 섞는다.
     *                     값의 유효성·중복은 컨트롤러에서 걸러져 들어온다.
     */
    public StationPlacesResponse getStationPlaces(Long stationId, List<String> travelStyles) {
        // 선택 파라미터라 안 보내면 null로 들어온다. 이후 분기를 하나로 두려고 빈 목록으로 맞춘다.
        List<String> selectedStyles = (travelStyles == null) ? List.of() : travelStyles;

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new CustomException(StationErrorCode.STATION_NOT_FOUND));

        // 코스 만들기는 뽑기 결과 역에서만 진입하므로 뽑기 대상이 아닌 역은 후보를 내리지 않는다.
        // 현재 데이터도 장소가 뽑기 역에만 붙어 있지만, 그 전제에 기대지 않고 조건을 명시한다.
        List<PlaceInfoResponse> allPlaces = station.isDrawable()
                ? placeQueryService.getPlacesByStation(stationId)
                : List.of();
        Map<String, List<PlaceInfoResponse>> placesByCategory = allPlaces.stream()
                .collect(Collectors.groupingBy(PlaceInfoResponse::categoryCode));

        Map<Long, List<String>> tagNamesByPlaceId = selectedStyles.isEmpty()
                ? Map.of()
                : placeInfoQueryService.getTagNamesByPlace(allPlaces.stream().map(PlaceInfoResponse::placeId).toList());

        List<StationPlaceCategoryResponse> categories = new ArrayList<>();
        for (String categoryCode : CATEGORY_DISPLAY_ORDER) {
            List<PlaceInfoResponse> places = placesByCategory.getOrDefault(categoryCode, List.of());
            if (places.isEmpty()) {
                continue;
            }
            List<PlaceInfoResponse> selected = selectCategoryPlaces(places, selectedStyles, tagNamesByPlaceId);
            // 카테고리 표시명은 카테고리의 속성이지만 별도 조회 창구가 없어 같은 그룹의 장소에서 가져온다.
            // 한 그룹의 장소는 모두 같은 카테고리이므로 어느 것을 써도 값은 같다.
            categories.add(stationConverter.toPlaceCategoryResponse(
                    categoryCode, selected.get(0).categoryName(), selected));
        }

        List<LineSummaryResponse> lines = groupLines(List.of(stationId)).getOrDefault(stationId, List.of());
        List<String> tags = resolveTopTags(allPlaces);
        String defaultCourseName = station.getStationName() + COURSE_NAME_SUFFIX;
        return stationConverter.toPlacesResponse(station, lines, tags, defaultCourseName, categories);
    }

    // 카테고리 후보가 최대 개수보다 많으면 잘라낸다.
    // travelStyles가 없으면 호출할 때마다 같은 결과가 나와야 선택 중인 사용자가 혼란스럽지 않다.
    // travelStyles가 있으면 태그 매칭 개수가 많은 순으로 정렬하되, 매칭 개수가 같은 장소끼리는 무작위로 섞는다.
    // 매칭 0개인 장소도 순위에 포함되므로 태그에 맞는 후보가 3개가 안 되면 자연히 무작위로 채워진다.
    private List<PlaceInfoResponse> selectCategoryPlaces(List<PlaceInfoResponse> places, List<String> travelStyles,
                                                          Map<Long, List<String>> tagNamesByPlaceId) {
        if (travelStyles.isEmpty()) {
            return places.stream()
                    .sorted(Comparator.comparing(PlaceInfoResponse::placeId))
                    .limit(PLACES_PER_CATEGORY)
                    .toList();
        }

        List<PlaceInfoResponse> shuffled = new ArrayList<>(places);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        return shuffled.stream()
                .sorted(Comparator.comparingInt(
                        (PlaceInfoResponse place) -> matchCount(place, travelStyles, tagNamesByPlaceId)).reversed())
                .limit(PLACES_PER_CATEGORY)
                .toList();
    }

    private int matchCount(PlaceInfoResponse place, List<String> travelStyles, Map<Long, List<String>> tagNamesByPlaceId) {
        List<String> placeTags = tagNamesByPlaceId.getOrDefault(place.placeId(), List.of());
        int count = 0;
        for (String travelStyle : travelStyles) {
            if (placeTags.contains(travelStyle)) {
                count++;
            }
        }
        return count;
    }

    // 역 대표 태그 = 그 역 장소들의 태그를 집계한 상위 3개
    private List<String> resolveTopTags(List<PlaceInfoResponse> places) {
        if (places.isEmpty()) {
            return List.of();
        }
        List<Long> placeIds = places.stream().map(PlaceInfoResponse::placeId).toList();
        return placeInfoQueryService.getTopTagNames(placeIds);
    }

    /**
     * 역 이름 검색(부분일치). "십리"로 검색하면 왕십리역·답십리역·상왕십리역이 모두 나온다.
     * "왕십리"와 "왕십리역" 중 무엇을 입력해도 같은 결과가 나오도록 꼬리의 "역"은 떼고 비교한다.
     * 결과가 없는 건 정상이므로 404가 아니라 빈 목록으로 응답한다.
     * 결과는 {@value #STATION_SEARCH_LIMIT}개로 제한한다.
     */
    public List<StationSummaryResponse> searchByName(String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<Station> stations = stationRepository.searchByNormalizedName(
                normalized, escapeLikePattern(normalized), PageRequest.of(0, STATION_SEARCH_LIMIT));
        if (stations.isEmpty()) {
            return List.of();
        }

        // 역마다 노선을 따로 조회하면 N+1이라 한 번에 묶어서 가져온다.
        List<Long> stationIds = stations.stream().map(Station::getId).toList();
        Map<Long, List<LineSummaryResponse>> linesByStationId = groupLines(stationIds);

        return stations.stream()
                .map(station -> stationConverter.toSummaryResponse(
                        station, linesByStationId.getOrDefault(station.getId(), List.of())))
                .toList();
    }

    // 서울 내 역은 모두 "역"으로 끝나 꼬리의 "역"은 역을 구분하지 못한다.
    // 떼고 비교해야 "왕십리"와 "왕십리역"이 같은 결과를 낸다.
    // 역삼역·역촌역처럼 이름 안쪽의 "역"은 그대로 두어야 하므로 꼬리에서 한 번만 뗀다.
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }
        String trimmed = keyword.trim();
        // "역" 한 글자는 떼지 않는다. 떼면 빈 검색어가 되는데, 이건 역삼역·동대문역사문화공원역처럼
        // 이름에 "역"이 든 역을 찾으려는 입력이다. 역명 쪽은 이미 꼬리를 뗐으므로 그 역들만 걸린다.
        if (trimmed.length() > STATION_NAME_SUFFIX.length() && trimmed.endsWith(STATION_NAME_SUFFIX)) {
            return trimmed.substring(0, trimmed.length() - STATION_NAME_SUFFIX.length());
        }
        return trimmed;
    }

    // 검색어에 든 LIKE 와일드카드를 문자 그대로 취급한다.
    // 이스케이프하지 않으면 "%" 한 글자로 전체 역이 조회된다.
    private String escapeLikePattern(String keyword) {
        return keyword.replace(LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
    }

    // 여러 역의 소속 노선을 stationId 기준으로 묶는다.
    private Map<Long, List<LineSummaryResponse>> groupLines(Collection<Long> stationIds) {
        return stationLineRepository.findLinesByStationIdIn(stationIds).stream()
                .collect(Collectors.groupingBy(
                        StationLineView::getStationId,
                        Collectors.mapping(lineConverter::toSummaryResponse, Collectors.toList())
                ));
    }

    public String getStationName(Long stationId) {
        return getStationNames(Set.of(stationId)).get(stationId);
    }

    public Map<Long, String> getStationNames(Collection<Long> stationIds) {
        return stationRepository.findAllById(stationIds).stream()
                .collect(Collectors.toMap(Station::getId, Station::getStationName));
    }

    /**
     * 주어진 역 id 순서 그대로 역 요약(이름 + 소속 노선)을 조회한다.
     * <p>
     * 다른 회원 스탬프 목록처럼 "최근 방문순"으로 이미 정렬된 id 목록을 받아
     * 그 순서를 유지한 채 역 정보만 채워 넣을 때 쓴다. {@code findAllById}는 순서를 보장하지 않는다.
     * 삭제 등으로 조회되지 않는 역 id는 결과에서 조용히 빠진다.
     */
    public List<StationSummaryResponse> getStationSummaries(List<Long> stationIds) {
        if (stationIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Station> stationById = stationRepository.findAllById(stationIds).stream()
                .collect(Collectors.toMap(Station::getId, station -> station));
        Map<Long, List<LineSummaryResponse>> linesByStationId = groupLines(stationIds);

        return stationIds.stream()
                .filter(stationById::containsKey)
                .map(stationId -> stationConverter.toSummaryResponse(
                        stationById.get(stationId), linesByStationId.getOrDefault(stationId, List.of())))
                .toList();
    }

    public LineSummaryResponse getLine(Long stationId) {
        return stationRepository.findById(stationId)
                .map(station -> {
                    // draw_line 우선, 없으면 StationLine에서 첫 번째
                    if (station.getDrawLine() != null) {
                        return lineConverter.toSummaryResponse(station.getDrawLine());
                    }
                    return stationLineRepository.findFirstByStationId(stationId)
                            .map(stationLine -> lineConverter.toSummaryResponse(stationLine.getLine()))
                            .orElse(null);
                })
                .orElse(null);
    }
}
